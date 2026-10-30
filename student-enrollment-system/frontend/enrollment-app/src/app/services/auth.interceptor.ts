import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!request.url.startsWith(environment.apiUrl + '/')) return next(request);
  const token = auth.token;
  return next(token ? request.clone({ setHeaders: { Authorization: 'Bearer ' + token } }) : request)
    .pipe(catchError(error => {
      if (error.status === 401) { auth.logout(); void router.navigateByUrl('/login'); }
      return throwError(() => error);
    }));
};
