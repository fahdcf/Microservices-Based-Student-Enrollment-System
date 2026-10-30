import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
export const authGuard: CanActivateFn = () => inject(AuthService).token ? true : inject(Router).createUrlTree(['/login']);
export const adminGuard: CanActivateFn = () => inject(AuthService).isAdmin ? true : inject(Router).createUrlTree(['/dashboard']);
