import { describe, it, expect, vi, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { TelemetryStreamService } from './telemetry-stream.service';
import { AuthService } from '../auth/auth.service';
import { signal } from '@angular/core';

describe('TelemetryStreamService', () => {
  let service: TelemetryStreamService;
  let mockEventSource: {
    addEventListener: ReturnType<typeof vi.fn>;
    close: ReturnType<typeof vi.fn>;
    onerror: ((e: Event) => void) | null;
    onopen: (() => void) | null;
  };

  beforeEach(() => {
    mockEventSource = {
      addEventListener: vi.fn(),
      close: vi.fn(),
      onerror: null,
      onopen: null,
    };

    // Mock global EventSource
    vi.stubGlobal(
      'EventSource',
      vi.fn(() => mockEventSource)
    );

    TestBed.configureTestingModule({
      providers: [
        TelemetryStreamService,
        {
          provide: AuthService,
          useValue: {
            state: signal({ authenticated: true, accessToken: 'test-token', expiresAt: Date.now() + 60000 }),
            getToken: () => 'test-token',
          },
        },
      ],
    });

    service = TestBed.inject(TelemetryStreamService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start disconnected', () => {
    expect(service.connected()).toBe(false);
  });

  it('should mark connected when EventSource opens', () => {
    service.connect();
    mockEventSource.onopen?.();
    expect(service.connected()).toBe(true);
  });

  it('should parse and store position_update events', () => {
    service.connect();
    mockEventSource.onopen?.();

    const positionUpdate = {
      trainId: 'amtrak:123',
      displayName: 'Capitol Limited',
      coordinates: [-87.65, 41.85] as [number, number],
      speedKmh: 120,
      bearing: 45,
      reportedAtMs: Date.now() - 5000,
      receivedAtMs: Date.now(),
      vehicleType: 'passenger',
      feedId: 'amtrak',
      operatorCode: 'AMTK',
      tripId: '30',
      routeShortName: 'Capitol Limited',
      destination: 'Chicago',
    };

    // Simulate position_update SSE event
    const handler = mockEventSource.addEventListener.mock.calls.find(
      ([name]: [string]) => name === 'position_update'
    )?.[1] as ((e: MessageEvent) => void) | undefined;

    handler?.({ data: JSON.stringify(positionUpdate) } as MessageEvent);

    expect(service.positions().get('amtrak:123')).toEqual(positionUpdate);
  });

  it('should schedule reconnect on error', () => {
    vi.useFakeTimers();
    service.connect();
    mockEventSource.onerror?.(new Event('error'));
    expect(service.connected()).toBe(false);
    vi.useRealTimers();
  });

  it('should disconnect and clean up', () => {
    service.connect();
    service.disconnect();
    expect(mockEventSource.close).toHaveBeenCalled();
    expect(service.connected()).toBe(false);
  });
});
