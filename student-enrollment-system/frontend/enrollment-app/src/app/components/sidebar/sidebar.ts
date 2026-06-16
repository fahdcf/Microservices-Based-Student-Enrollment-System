import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class SidebarComponent {
  navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: '⬡' },
    { path: '/students',  label: 'Students',  icon: '◈' },
    { path: '/courses',   label: 'Courses',   icon: '◇' },
    { path: '/enroll',    label: 'Enrollment',icon: '◉' },
  ];
}
