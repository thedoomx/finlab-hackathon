import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { API_CONFIG } from '../config/api-config';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const mockApiConfig = { baseUrl: 'http://localhost:8081' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: API_CONFIG, useValue: mockApiConfig }
      ]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should send login request with username', () => {
    const username = 'testuser';
    const mockToken = 'mock-jwt-token';

    service.login(username).subscribe(token => {
      expect(token).toBe(mockToken);
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username });
    req.flush(mockToken);
  });

  it('should handle login error', () => {
    const username = 'testuser';

    service.login(username).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(401);
      }
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/auth/login`);
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
  });

  it('should send logout request', () => {
    service.logout().subscribe(response => {
      expect(response).toBeNull();
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/auth/logout`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(null);
  });

  it('should handle logout error', () => {
    service.logout().subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(500);
      }
    });

    const req = httpMock.expectOne(`${mockApiConfig.baseUrl}/auth/logout`);
    req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });
  });
});
