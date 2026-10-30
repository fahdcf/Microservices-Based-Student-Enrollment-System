import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('Authentication', () => {
  let auth: AuthService; let http: HttpClient; let requests: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()
    ] });
    auth = TestBed.inject(AuthService); http = TestBed.inject(HttpClient); requests = TestBed.inject(HttpTestingController);
  });
  afterEach(() => requests.verify());
  function login() {
    auth.login('student', 'password').subscribe();
    requests.expectOne('/api/auth/login').flush({ accessToken: 'test-token', expiresIn: 900, role: 'STUDENT', cnie: 'AB1234', studentId: 1 });
  }
  it('adds bearer credentials only to the application API', () => {
    login(); expect(auth.isAdmin).toBe(false);
    http.get('/api/courses').subscribe();
    const api = requests.expectOne('/api/courses');
    expect(api.request.headers.get('Authorization')).toBe('Bearer test-token'); api.flush([]);
    http.get('https://example.test/data').subscribe();
    const other = requests.expectOne('https://example.test/data');
    expect(other.request.headers.has('Authorization')).toBe(false); other.flush({});
  });
  it('clears expired sessions and logs out on a rejected token', () => {
    login();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    http.get('/api/courses').subscribe({ error: () => {} });
    requests.expectOne('/api/courses').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.token).toBeNull(); expect(navigate).toHaveBeenCalledWith('/login');
  });
  it('expires the in-memory token', () => {
    vi.useFakeTimers();
    login(); vi.advanceTimersByTime(901000);
    expect(auth.token).toBeNull(); vi.useRealTimers();
  });
});
