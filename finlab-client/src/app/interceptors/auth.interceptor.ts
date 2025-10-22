import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TokenService } from '../services/token.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  private readonly API_KEY = 'finlab-validator-test-api-key';

  constructor(private tokenService: TokenService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    let headers = req.headers.set('X-API-KEY', this.API_KEY);

    const token = this.tokenService.get();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    const authReq = req.clone({ headers });
    return next.handle(authReq);
  }
}
