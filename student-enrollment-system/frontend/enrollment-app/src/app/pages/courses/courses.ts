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

  formData: CourseRequest = { title: '', description: '', credits: 1 };

  constructor(private courseService: CourseService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.courseService.getAll().subscribe({
      next: (data) => { this.courses = data; this.loading = false; },
      error: () => { this.error = 'Failed to load courses.'; this.loading = false; }
    });
  }

  submit(form: NgForm) {
    if (form.invalid) return;
    this.courseService.create(this.formData).subscribe({
      next: () => {
        this.success = 'Course created successfully!';
        this.error = '';
        this.showForm = false;
        form.reset();
        this.formData = { title: '', description: '', credits: 1 };
        this.load();
      },
      error: (err) => {
        this.error = err.error?.error || 'Failed to create course.';
        this.success = '';
      }
    });
  }

  delete(id: number) {
    if (!confirm('Delete this course?')) return;
    this.courseService.delete(id).subscribe({
      next: () => this.load(),
      error: (err) => this.error = err.error?.error || 'Cannot delete course.'
    });
  }
}
