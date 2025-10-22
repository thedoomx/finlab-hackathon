import {Inject, Injectable} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {API_CONFIG, ApiConfig} from "../config/api-config";

@Injectable({
  providedIn: 'root'
})
export class AccountService {

  constructor(private http: HttpClient, @Inject(API_CONFIG) private config: ApiConfig) {}

  ibanLookup(iban: string): Observable<any> {
    return this.http.get<any>(`${this.config.baseUrl}/api/${this.config.apiVersion}/accounts/${iban}`);
  }
}
