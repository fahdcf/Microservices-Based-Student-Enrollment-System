import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss'
})
export class NavbarComponent {
  navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: '⬡' },
    { path: '/students',  label: 'Students',  icon: '◈' },
    { path: '/courses',   label: 'Courses',   icon: '◇' },
    { path: '/enroll',    label: 'Enroll',    icon: '◉' },
  ];
}
