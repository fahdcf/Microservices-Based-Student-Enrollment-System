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
    this.error = '';
    this.courseService.getAll().subscribe({
      next: data => { this.courses = data; this.loading = false; },
      error: err => { this.error = this.extractError(err); this.loading = false; }
    });
  }

  submit(form: NgForm) {
    if (form.invalid) return;
    this.error = '';
    this.success = '';
    this.courseService.create(this.formData).subscribe({
      next: created => {
        this.success = `Course "${created.title}" created successfully!`;
        this.showForm = false;
        form.reset();
        this.formData = { title: '', description: '', credits: 1 };
        this.load();
        setTimeout(() => this.success = '', 4000);
      },
      error: err => {
        this.error = this.extractError(err);
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
      error: err => this.error = this.extractError(err)
    });
  }

  private extractError(err: any): string {
    if (!err?.error) return `Request failed (HTTP ${err?.status ?? 'unknown'}).`;
    if (typeof err.error === 'string') return err.error;
    if (err.error?.error) return err.error.error;
    const entries = Object.entries(err.error);
    if (entries.length) return entries.map(([k, v]) => `${k}: ${v}`).join(' | ');
    return `Unexpected error (HTTP ${err.status}).`;
  }
}
