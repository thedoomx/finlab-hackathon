import { InjectionToken } from '@angular/core';

export interface ApiConfig {
  baseUrl: string;
  apiVersion: string;
}

export const API_CONFIG = new InjectionToken<ApiConfig>('api.config');
