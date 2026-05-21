import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StudentService } from '../../services/student.service';
import { EnrollmentService } from '../../services/enrollment.service';
import { CourseService } from '../../services/course.service';
import { Enrollment } from '../../models/enrollment.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class DashboardComponent implements OnInit {
  totalStudents = 0;
  totalCourses = 0;
  statsError = '';

  searchCnie = '';
  enrollments: Enrollment[] = [];
  loading = false;
  error = '';
  searched = false;

  constructor(
    private studentService: StudentService,
    private enrollmentService: EnrollmentService,
    private courseService: CourseService
  ) {}

  ngOnInit() {
    this.studentService.getAll().subscribe({
      next: s => this.totalStudents = s.length,
      error: () => this.statsError = 'Could not reach backend. Make sure all services are running.'
    });
    this.courseService.getAll().subscribe({
      next: c => this.totalCourses = c.length,
      error: () => {}
    });
  }

  search() {
    if (!this.searchCnie.trim()) return;
    this.loading = true;
    this.error = '';
    this.searched = false;
    this.enrollments = [];

    this.enrollmentService.getByStudentCnie(this.searchCnie.trim()).subscribe({
      next: data => {
        this.enrollments = data;
        this.searched = true;
        this.loading = false;
      },
      error: err => {
        this.error = this.extractError(err);
        this.searched = true;
        this.loading = false;
      }
    });
  }

  cancel(enrollmentId: number) {
    this.error = '';
    this.enrollmentService.cancel(enrollmentId).subscribe({
      next: () => {
        this.enrollments = this.enrollments.filter(e => e.enrollmentId !== enrollmentId);
      },
      error: err => {
        this.error = this.extractError(err);
      }
    });
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('fr-MA', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
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
