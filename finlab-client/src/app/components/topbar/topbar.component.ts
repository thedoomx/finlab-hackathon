import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { TokenService } from '../../services/token.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-topbar',
  templateUrl: './topbar.component.html'
})
export class TopbarComponent {

  constructor(
    public tokenService: TokenService,
    private authService: AuthService,
    private router: Router
  ) {}

  get isLoggedIn(): boolean {
    return this.tokenService.exists();
  }

  logout() {
    this.authService.logout().subscribe({
      next: () => {
        this.tokenService.clear();
        this.router.navigate(['/login']);
      },
      error: () => {
        this.tokenService.clear();
        this.router.navigate(['/login']);
      }
    });
  }
}
