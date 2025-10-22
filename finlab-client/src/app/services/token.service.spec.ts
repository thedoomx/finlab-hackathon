import { TestBed } from '@angular/core/testing';
import { TokenService } from './token.service';

describe('TokenService', () => {
  let service: TokenService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TokenService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should store token in memory', () => {
    const token = 'test-jwt-token';
    service.set(token);
    expect(service.get()).toBe(token);
  });

  it('should return null when no token is set', () => {
    expect(service.get()).toBeNull();
  });

  it('should clear token', () => {
    service.set('test-token');
    service.clear();
    expect(service.get()).toBeNull();
  });

  it('should return true when token exists', () => {
    service.set('test-token');
    expect(service.exists()).toBe(true);
  });

  it('should return false when token does not exist', () => {
    expect(service.exists()).toBe(false);
  });

  it('should return false after clearing token', () => {
    service.set('test-token');
    service.clear();
    expect(service.exists()).toBe(false);
  });
});
