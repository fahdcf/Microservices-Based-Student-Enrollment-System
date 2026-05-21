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
    this.studentService.getAll().subscribe(s => this.totalStudents = s.length);
    this.courseService.getAll().subscribe(c => this.totalCourses = c.length);
  }

  search() {
    if (!this.searchCnie.trim()) return;
    this.loading = true;
    this.error = '';
    this.searched = false;

    this.enrollmentService.getByStudentCnie(this.searchCnie.trim()).subscribe({
      next: (data) => {
        this.enrollments = data;
        this.searched = true;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.error || 'Student not found.';
        this.enrollments = [];
        this.searched = true;
        this.loading = false;
      }
    });
  }

  cancel(enrollmentId: number) {
    this.enrollmentService.cancel(enrollmentId).subscribe({
      next: () => {
        this.enrollments = this.enrollments.filter(e => e.enrollmentId !== enrollmentId);
      },
      error: (err) => {
        this.error = err.error?.error || 'Cannot cancel enrollment.';
      }
    });
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('fr-MA', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
