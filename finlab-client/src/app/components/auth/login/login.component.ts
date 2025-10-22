import { Component } from '@angular/core';
import { Router } from '@angular/router';
import {TokenService} from "../../../services/token.service";
import {AuthService} from "../../../services/auth.service";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html'
})
export class LoginComponent {
  username = '';
  error: string | null = null;

  constructor(private authService: AuthService, private tokenService: TokenService, private router: Router) {}

  login() {
    this.authService.login(this.username).subscribe({
      next: token => {
        this.tokenService.set(token);
        this.router.navigate(['/accounts']);
      },
      error: err => {
        this.error = 'Login failed';
      }
    });
  }
}
