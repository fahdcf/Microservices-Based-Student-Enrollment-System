import { apiErrorMessage } from '../../services/api-error';
import { AuthService } from '../../services/auth.service';
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

  constructor(public auth: AuthService,
    private courseService: CourseService,
    private enrollmentService: EnrollmentService
  ) {}

  ngOnInit() {
    this.studentCnie = this.auth.session()?.cnie || '';
    this.courseService.getAll().subscribe({
      next: data => { this.courses = data; this.coursesLoading = false; },
      error: err => {
        this.error = 'Could not load courses: ' + apiErrorMessage(err);
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
        this.error = apiErrorMessage(err);
        this.loading = false;
      }
    });
  }

  getSelectedCourse(): Course | undefined {
    return this.courses.find(c => c.id === this.selectedCourseId);
  }
}
