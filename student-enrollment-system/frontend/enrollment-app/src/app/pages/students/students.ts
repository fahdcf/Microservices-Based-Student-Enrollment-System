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
    this.studentService.getAll().subscribe({
      next: (data) => { this.students = data; this.loading = false; },
      error: () => { this.error = 'Failed to load students.'; this.loading = false; }
    });
  }

  submit(form: NgForm) {
    if (form.invalid) return;
    this.studentService.create(this.formData).subscribe({
      next: () => {
        this.success = 'Student registered successfully!';
        this.error = '';
        this.showForm = false;
        form.reset();
        this.formData = { cnie: '', firstName: '', lastName: '', email: '' };
        this.load();
      },
      error: (err) => {
        this.error = err.error?.error || 'Failed to create student.';
        this.success = '';
      }
    });
  }

  delete(id: number) {
    if (!confirm('Delete this student?')) return;
    this.studentService.delete(id).subscribe({
      next: () => this.load(),
      error: (err) => this.error = err.error?.error || 'Cannot delete student.'
    });
  }
}
