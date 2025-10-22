import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_CONFIG, ApiConfig } from '../config/api-config';

export interface TestResultListItem {
  testId: string;
  testName: string;
  executionDate: string;
  fileName: string;
}

export interface TestResultSummary {
  testId: string;
  testName: string;
  executionDate: string;
  totalRequests: number;
  successfulRequests: number;
  failedRequests: number;
  errorRate: number;
  averageResponseTime: number;
  minResponseTime: number;
  maxResponseTime: number;
  p90ResponseTime: number;
  p95ResponseTime: number;
  throughput: number;
}

@Injectable({
  providedIn: 'root'
})
export class StressTestResultService {

  constructor(
    private http: HttpClient,
    @Inject(API_CONFIG) private config: ApiConfig
  ) {}

  listTestResults(): Observable<TestResultListItem[]> {
    return this.http.get<TestResultListItem[]>(`${this.config.baseUrl}/results`);
  }

  getTestResultSummary(testId: string): Observable<TestResultSummary> {
    return this.http.get<TestResultSummary>(`${this.config.baseUrl}/results/${testId}`);
  }
}
