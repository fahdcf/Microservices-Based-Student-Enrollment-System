export interface Enrollment {
  enrollmentId: number;
  studentCnie: string;
  courseName: string;
  courseCredits: number;
  enrollmentDate: string;
  deletable: boolean;
}

export interface EnrollmentRequest {
  studentCnie: string;
  courseId: number;
}
