import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Session { accessToken: string; expiresIn: number; role: 'ADMIN' | 'STUDENT'; cnie: string | null; studentId: number | null; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  // Keep bearer tokens in memory; a reload requires signing in again.
  readonly session = signal<Session | null>(null);
  private expiresAt = 0;
  constructor(private http: HttpClient) {}
  get token() {
    if (Date.now() >= this.expiresAt) { this.session.set(null); return null; }
    return this.session()?.accessToken ?? null;
  }
  get isAdmin() { return this.session()?.role === 'ADMIN'; }
  login(username: string, password: string) {
    return this.http.post<Session>(environment.apiUrl + '/auth/login', { username, password }).pipe(tap(s => {
      this.expiresAt = Date.now() + s.expiresIn * 1000;
      this.session.set(s);
    }));
  }
  logout() { this.expiresAt = 0; this.session.set(null); }
}
