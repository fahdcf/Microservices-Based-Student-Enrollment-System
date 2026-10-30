import { Routes } from '@angular/router';

import { authGuard, adminGuard } from './services/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login').then(m => m.LoginComponent) },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'dashboard', canActivate: [authGuard],
    loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.DashboardComponent)
  },
  {
    path: 'students', canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./pages/students/students').then(m => m.StudentsComponent)
  },
  {
    path: 'courses', canActivate: [authGuard],
    loadComponent: () => import('./pages/courses/courses').then(m => m.CoursesComponent)
  },
  {
    path: 'enroll', canActivate: [authGuard],
    loadComponent: () => import('./pages/enroll/enroll').then(m => m.EnrollComponent)
  },
  { path: '**', redirectTo: '/dashboard' }
];
