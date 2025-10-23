import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { StressTestResultService, TestResultListItem, TestResultSummary } from './stress-test-result.service';
import { API_CONFIG } from '../config/api-config';

describe('StressTestResultService', () => {
  let service: StressTestResultService;
  let httpMock: HttpTestingController;
  const mockApiConfig = { baseUrl: 'http://localhost:8081', apiVersion: 'v1' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        StressTestResultService,
        { provide: API_CONFIG, useValue: mockApiConfig }
      ]
    });
    service = TestBed.inject(StressTestResultService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should list test results', () => {
    const mockResults: TestResultListItem[] = [
      {
        testId: 'test1',
        testName: 'Normal Load Test',
        executionDate: '2025-10-21T10:00:00',
        fileName: 'test1.jtl'
      },
      {
        testId: 'test2',
        testName: 'Extreme Load Test',
        executionDate: '2025-10-21T11:00:00',
        fileName: 'test2.jtl'
      }
    ];

    service.listTestResults().subscribe(results => {
      expect(results).toEqual(mockResults);
      expect(results.length).toBe(2);
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/api/${mockApiConfig.apiVersion}/results`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResults);
  });

  it('should return empty array when no test results exist', () => {
    service.listTestResults().subscribe(results => {
      expect(results).toEqual([]);
      expect(results.length).toBe(0);
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/api/${mockApiConfig.apiVersion}/results`);
    req.flush([]);
  });

  it('should get test result summary by testId', () => {
    const testId = 'test1';
    const mockSummary: TestResultSummary = {
      testId: 'test1',
      testName: 'Normal Load Test',
      executionDate: '2025-10-21T10:00:00',
      totalRequests: 1000,
      successfulRequests: 950,
      failedRequests: 50,
      errorRate: 5.0,
      averageResponseTime: 250.5,
      minResponseTime: 100,
      maxResponseTime: 1500,
      p90ResponseTime: 450,
      p95ResponseTime: 600,
      throughput: 100
    };

    service.getTestResultSummary(testId).subscribe(summary => {
      expect(summary).toEqual(mockSummary);
      expect(summary.totalRequests).toBe(1000);
      expect(summary.errorRate).toBe(5.0);
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/api/${mockApiConfig.apiVersion}/results/${testId}`);
    expect(req.request.method).toBe('GET');
    req.flush(mockSummary);
  });

  it('should handle 404 error for non-existent testId', () => {
    const testId = 'non-existent';

    service.getTestResultSummary(testId).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(404);
      }
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/api/${mockApiConfig.apiVersion}/results/${testId}`);
    req.flush('Not Found', { status: 404, statusText: 'Not Found' });
  });

  it('should handle unauthorized error when listing results', () => {
    service.listTestResults().subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(401);
      }
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/api/${mockApiConfig.apiVersion}/results`);
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });

  it('should handle server error when getting summary', () => {
    const testId = 'test1';

    service.getTestResultSummary(testId).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(500);
      }
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/api/${mockApiConfig.apiVersion}/results/${testId}`);
    req.flush('Internal Server Error', { status: 500, statusText: 'Internal Server Error' });
  });
});
