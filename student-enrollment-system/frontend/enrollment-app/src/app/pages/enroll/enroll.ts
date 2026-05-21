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
      next: (data) => this.courses = data,
      error: () => this.error = 'Failed to load courses.'
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
      courseId: this.selectedCourseId
    }).subscribe({
      next: (data) => {
        this.result = data;
        this.success = 'Enrollment successful!';
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.error || 'Enrollment failed.';
        this.loading = false;
      }
    });
  }

  getSelectedCourse(): Course | undefined {
    return this.courses.find(c => c.id === this.selectedCourseId);
  }
}
