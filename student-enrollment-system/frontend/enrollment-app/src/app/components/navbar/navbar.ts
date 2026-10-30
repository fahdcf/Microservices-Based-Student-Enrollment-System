import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar', standalone: true, imports: [CommonModule],
  templateUrl: './navbar.html', styleUrl: './navbar.scss'
})
export class NavbarComponent {
  constructor(public auth: AuthService, private router: Router) {}
  logout() { this.auth.logout(); void this.router.navigateByUrl('/login'); }
}
