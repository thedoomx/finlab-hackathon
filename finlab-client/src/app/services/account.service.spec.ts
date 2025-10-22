import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AccountService } from './account.service';
import { API_CONFIG } from '../config/api-config';

describe('AccountService', () => {
  let service: AccountService;
  let httpMock: HttpTestingController;
  const mockApiConfig = { baseUrl: 'http://localhost:8081' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AccountService,
        { provide: API_CONFIG, useValue: mockApiConfig }
      ]
    });
    service = TestBed.inject(AccountService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should perform IBAN lookup with valid IBAN', () => {
    const iban = 'GB33BUKB20201555555555';
    const mockResponse = 'ALLOW';

    service.ibanLookup(iban).subscribe(response => {
      expect(response).toBe(mockResponse);
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/accounts/${iban}`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should handle IBAN lookup with BLOCK status', () => {
    const iban = 'FR1420041010050500013M02606';
    const mockResponse = 'BLOCK';

    service.ibanLookup(iban).subscribe(response => {
      expect(response).toBe(mockResponse);
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/accounts/${iban}`);
    req.flush(mockResponse);
  });

  it('should handle IBAN lookup with REVIEW status', () => {
    const iban = 'DE89370400440532013000';
    const mockResponse = 'REVIEW';

    service.ibanLookup(iban).subscribe(response => {
      expect(response).toBe(mockResponse);
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/accounts/${iban}`);
    req.flush(mockResponse);
  });

  it('should handle 404 error for non-existent IBAN', () => {
    const iban = 'INVALID123';

    service.ibanLookup(iban).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(404);
      }
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/accounts/${iban}`);
    req.flush('Not Found', { status: 404, statusText: 'Not Found' });
  });

  it('should handle 401 unauthorized error', () => {
    const iban = 'GB33BUKB20201555555555';

    service.ibanLookup(iban).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(401);
      }
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/accounts/${iban}`);
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });
});
