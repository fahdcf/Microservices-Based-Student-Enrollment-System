import { AuthService } from '../../services/auth.service';
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { StudentService } from '../../services/student.service';
import { Student, StudentRequest } from '../../models/student.model';

@Component({
  selector: 'app-students',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './students.html',
  styleUrl: './students.scss'
})
export class StudentsComponent implements OnInit {
  students: Student[] = [];
  loading = false;
  error = '';
  success = '';
  showForm = false;
  editingId: number | null = null;

  formData: StudentRequest = { cnie: '', firstName: '', lastName: '', email: '' };

  constructor(public auth: AuthService, private http: HttpClient, private studentService: StudentService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.error = '';
    this.studentService.getAll().subscribe({
      next: data => { this.students = data; this.loading = false; },
      error: err => { this.error = this.extractError(err); this.loading = false; }
    });
  }

  startCreate() {
    this.editingId = null;
    this.formData = { cnie: '', firstName: '', lastName: '', email: '' };
    this.showForm = true; this.error = ''; this.success = '';
  }

  edit(item: Student) {
    this.editingId = item.id;
    this.formData = { ...item };
    this.showForm = true; this.error = ''; this.success = '';
  }

  submit(form: NgForm) {
    if (form.invalid) return;
    this.error = '';
    this.success = '';
    const operation = this.editingId === null
      ? this.studentService.create(this.formData)
      : this.studentService.update(this.editingId, this.formData);
    operation.subscribe({
      next: () => {
        this.success = 'Student saved successfully.';
        this.showForm = false;
        this.editingId = null;
        form.reset();
        this.formData = { cnie: '', firstName: '', lastName: '', email: '' };
        this.load();
        setTimeout(() => this.success = '', 4000);
      },
      error: err => {
        this.error = this.extractError(err);
      }
    });
  }

  delete(id: number, cnie: string) {
    if (!confirm(`Delete student ${cnie}? This cannot be undone.`)) return;
    this.error = '';
    this.studentService.delete(id).subscribe({
      next: () => {
        this.success = `Student ${cnie} deleted.`;
        this.load();
        setTimeout(() => this.success = '', 3000);
      },
      error: err => this.error = this.extractError(err)
    });
  }

  accountStudent: Student | null = null;
  accountUsername = '';
  accountPassword = '';
  createAccount(student: Student) { this.accountStudent = student; this.accountUsername = ''; this.accountPassword = ''; }
  saveAccount() {
    const student = this.accountStudent;
    if (!student) return;
    const username = this.accountUsername;
    const password = this.accountPassword;
    this.http.post(environment.apiUrl + '/auth/accounts', { username, password, studentId: student.id }).subscribe({
      next: () => { this.success = 'Student account created.'; this.accountStudent = null; this.accountPassword = ''; },
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
