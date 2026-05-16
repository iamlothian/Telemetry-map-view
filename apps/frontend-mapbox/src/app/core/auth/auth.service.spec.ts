import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AuthService } from './auth.service';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    // Clean storage between tests
    sessionStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        {
          provide: HttpClient,
          useValue: { post: vi.fn() },
        },
      ],
    });
    service = TestBed.inject(AuthService);
  });

  it('should start unauthenticated when no token in session storage', () => {
    expect(service.state().authenticated).toBe(false);
    expect(service.getToken()).toBeNull();
  });

  it('should read stored token as authenticated', () => {
    const future = Date.now() + 60_000;
    sessionStorage.setItem('tmv_access_token', 'stored-token');
    sessionStorage.setItem('tmv_token_expires', String(future));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: HttpClient, useValue: { post: vi.fn() } },
      ],
    });
    const fresh = TestBed.inject(AuthService);

    expect(fresh.state().authenticated).toBe(true);
    expect(fresh.getToken()).toBe('stored-token');
  });

  it('should treat expired token as unauthenticated', () => {
    const past = Date.now() - 1000;
    sessionStorage.setItem('tmv_access_token', 'expired-token');
    sessionStorage.setItem('tmv_token_expires', String(past));

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: HttpClient, useValue: { post: vi.fn() } },
      ],
    });
    const fresh = TestBed.inject(AuthService);

    expect(fresh.state().authenticated).toBe(false);
  });

  it('should clear state on logout', () => {
    sessionStorage.setItem('tmv_access_token', 'some-token');
    sessionStorage.setItem('tmv_token_expires', String(Date.now() + 60_000));

    // Mock window.location.href assignment
    const originalLocation = window.location;
    vi.spyOn(window, 'location', 'get').mockReturnValue({
      ...originalLocation,
      href: '',
      origin: 'http://localhost',
    } as Location);
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { href: '', origin: 'http://localhost' },
    });

    service.logout();

    expect(service.state().authenticated).toBe(false);
    expect(service.getToken()).toBeNull();
  });
});
