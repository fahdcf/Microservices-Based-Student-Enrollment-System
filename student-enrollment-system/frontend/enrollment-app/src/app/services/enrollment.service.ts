import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Enrollment, EnrollmentRequest } from '../models/enrollment.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class EnrollmentService {
  private readonly url = `${environment.apiUrl}/enrollments`;

  constructor(private http: HttpClient) {}

  enroll(request: EnrollmentRequest): Observable<Enrollment> {
    return this.http.post<Enrollment>(this.url, request);
  }

  getByStudentCnie(cnie: string): Observable<Enrollment[]> {
    return this.http.get<Enrollment[]>(`${this.url}/student/${cnie}`);
  }

  cancel(enrollmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${enrollmentId}`);
  }
}
