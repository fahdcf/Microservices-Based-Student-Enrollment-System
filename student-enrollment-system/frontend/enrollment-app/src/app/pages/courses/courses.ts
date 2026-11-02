import { apiErrorMessage } from '../../services/api-error';
import { AuthService } from '../../services/auth.service';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { CourseService } from '../../services/course.service';
import { Course, CourseRequest } from '../../models/course.model';

@Component({
  selector: 'app-courses',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './courses.html',
  styleUrl: './courses.scss'
})
export class CoursesComponent implements OnInit {
  courses: Course[] = [];
  loading = false;
  error = '';
  success = '';
  showForm = false;
  editingId: number | null = null;

  formData: CourseRequest = { title: '', description: '', credits: 1 };

  constructor(public auth: AuthService, private courseService: CourseService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.error = '';
    this.courseService.getAll().subscribe({
      next: data => { this.courses = data; this.loading = false; },
      error: err => { this.error = apiErrorMessage(err); this.loading = false; }
    });
  }

  startCreate() {
    this.editingId = null;
    this.formData = { title: '', description: '', credits: 1 };
    this.showForm = true; this.error = ''; this.success = '';
  }

  edit(item: Course) {
    this.editingId = item.id;
    this.formData = { ...item };
    this.showForm = true; this.error = ''; this.success = '';
  }

  submit(form: NgForm) {
    if (form.invalid) return;
    this.error = '';
    this.success = '';
    const operation = this.editingId === null
      ? this.courseService.create(this.formData)
      : this.courseService.update(this.editingId, this.formData);
    operation.subscribe({
      next: () => {
        this.success = 'Course saved successfully.';
        this.showForm = false;
        this.editingId = null;
        form.reset();
        this.formData = { title: '', description: '', credits: 1 };
        this.load();
        setTimeout(() => this.success = '', 4000);
      },
      error: err => {
        this.error = apiErrorMessage(err);
      }
    });
  }

  delete(id: number, title: string) {
    if (!confirm(`Delete course "${title}"? This cannot be undone.`)) return;
    this.error = '';
    this.courseService.delete(id).subscribe({
      next: () => {
        this.success = `Course "${title}" deleted.`;
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: err => this.error = apiErrorMessage(err)
    });
  }
}
