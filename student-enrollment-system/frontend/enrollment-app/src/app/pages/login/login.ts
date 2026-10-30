import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  standalone: true, imports: [CommonModule, FormsModule],
  template: `
    <div class="page-container"><div class="card" style="max-width:420px;margin:3rem auto;padding:2rem">
      <h1>Sign in</h1>
      <p>Use the account provided by your administrator.</p>
      <form #form="ngForm" (ngSubmit)="login()">
        <div class="form-field"><label for="username">Username</label>
          <input id="username" name="username" [(ngModel)]="username" required autocomplete="username"></div>
        <div class="form-field"><label for="password">Password</label>
          <input id="password" name="password" type="password" [(ngModel)]="password" required autocomplete="current-password"></div>
        <p *ngIf="error" class="alert alert-error">{{ error }}</p>
        <button class="btn btn-primary" [disabled]="form.invalid || loading">{{ loading ? 'Signing in...' : 'Sign in' }}</button>
      </form>
    </div></div>
  `
})
export class LoginComponent {
  username = ''; password = ''; error = ''; loading = false;
  constructor(private auth: AuthService, private router: Router) {}
  login() {
    if (!this.username || !this.password || this.loading) return;
    this.loading = true; this.error = '';
    this.auth.login(this.username, this.password).subscribe({
      next: () => { this.password = ''; void this.router.navigateByUrl('/dashboard'); },
      error: err => { this.error = err.error?.error || 'Unable to sign in. Please try again.'; this.loading = false; }
    });
  }
}
