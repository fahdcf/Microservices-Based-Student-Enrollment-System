import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard/dashboard').then(m => m.DashboardComponent)
  },
  {
    path: 'students',
    loadComponent: () => import('./pages/students/students').then(m => m.StudentsComponent)
  },
  {
    path: 'courses',
    loadComponent: () => import('./pages/courses/courses').then(m => m.CoursesComponent)
  },
  {
    path: 'enroll',
    loadComponent: () => import('./pages/enroll/enroll').then(m => m.EnrollComponent)
  },
  { path: '**', redirectTo: '/dashboard' }
];
