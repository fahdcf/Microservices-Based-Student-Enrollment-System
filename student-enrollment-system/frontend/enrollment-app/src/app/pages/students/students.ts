import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
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

  formData: StudentRequest = { cnie: '', firstName: '', lastName: '', email: '' };

  constructor(private studentService: StudentService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.error = '';
    this.studentService.getAll().subscribe({
      next: data => { this.students = data; this.loading = false; },
      error: err => { this.error = this.extractError(err); this.loading = false; }
    });
  }

  submit(form: NgForm) {
    if (form.invalid) return;
    this.error = '';
    this.success = '';
    this.studentService.create(this.formData).subscribe({
      next: () => {
        this.success = `Student ${this.formData.cnie} registered successfully!`;
        this.showForm = false;
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

  private extractError(err: any): string {
    if (!err?.error) return `Request failed (HTTP ${err?.status ?? 'unknown'}).`;
    if (typeof err.error === 'string') return err.error;
    if (err.error?.error) return err.error.error;
    const entries = Object.entries(err.error);
    if (entries.length) return entries.map(([k, v]) => `${k}: ${v}`).join(' | ');
    return `Unexpected error (HTTP ${err.status}).`;
  }
}
