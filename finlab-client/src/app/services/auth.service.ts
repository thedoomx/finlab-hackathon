import {Inject, Injectable} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {API_CONFIG, ApiConfig} from "../config/api-config";

@Injectable({
  providedIn: 'root'
})
export class AuthService {


  constructor(private http: HttpClient, @Inject(API_CONFIG) private config: ApiConfig) {}

  login(username: string): Observable<string> {
    return this.http.post(`${this.config.baseUrl}/auth/login`, { username }, { responseType: 'text' });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.config.baseUrl}/auth/logout`, {});
  }
}
