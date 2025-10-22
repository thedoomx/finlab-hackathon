import { Injectable } from '@angular/core';
import {Subject} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private token: string | null = null;
  tokenCleared$ = new Subject<void>();

  set(token: string) {
    this.token = token;
  }

  get(): string | null {
    return this.token;
  }

  clear() {
    this.token = null;
    this.tokenCleared$.next();
  }

  exists(): boolean {
    return !!this.token;
  }
}
