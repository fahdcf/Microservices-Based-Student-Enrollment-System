import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Student, StudentRequest } from '../models/student.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly url = `${environment.apiUrl}/students`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Student[]> {
    return this.http.get<Student[]>(this.url);
  }

  getByCnie(cnie: string): Observable<Student> {
    return this.http.get<Student>(`${this.url}/cnie/${cnie}`);
  }

  create(request: StudentRequest): Observable<Student> {
    return this.http.post<Student>(this.url, request);
  }

  update(id: number, request: StudentRequest): Observable<Student> {
    return this.http.put<Student>(this.url + '/' + id, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
