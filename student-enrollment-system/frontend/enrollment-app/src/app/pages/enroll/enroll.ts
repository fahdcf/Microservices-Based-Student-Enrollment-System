import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CourseService } from '../../services/course.service';
import { EnrollmentService } from '../../services/enrollment.service';
import { Course } from '../../models/course.model';
import { Enrollment } from '../../models/enrollment.model';

@Component({
  selector: 'app-enroll',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './enroll.html',
  styleUrl: './enroll.scss'
})
export class EnrollComponent implements OnInit {
  courses: Course[] = [];
  coursesLoading = true;
  selectedCourseId: number | null = null;
  studentCnie = '';
  loading = false;
  error = '';
  success = '';
  result: Enrollment | null = null;

  constructor(
    private courseService: CourseService,
    private enrollmentService: EnrollmentService
  ) {}

  ngOnInit() {
    this.courseService.getAll().subscribe({
      next: data => { this.courses = data; this.coursesLoading = false; },
      error: err => {
        this.error = 'Could not load courses: ' + this.extractError(err);
        this.coursesLoading = false;
      }
    });
  }

  enroll() {
    if (!this.studentCnie.trim() || !this.selectedCourseId) return;
    this.loading = true;
    this.error = '';
    this.success = '';
    this.result = null;

    this.enrollmentService.enroll({
      studentCnie: this.studentCnie.trim(),
      courseId: Number(this.selectedCourseId)
    }).subscribe({
      next: data => {
        this.result = data;
        this.success = `Successfully enrolled in "${data.courseName}"!`;
        this.loading = false;
        setTimeout(() => this.success = '', 5000);
      },
      error: err => {
        this.error = this.extractError(err);
        this.loading = false;
      }
    });
  }

  getSelectedCourse(): Course | undefined {
    return this.courses.find(c => c.id === this.selectedCourseId);
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
