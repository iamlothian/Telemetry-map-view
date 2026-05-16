import { inject, Injectable, OnDestroy, signal } from '@angular/core';
import { FeedStatus, TelemetryEvent, TrainPosition } from 'shared-models';
import { AuthService } from '../auth/auth.service';
import { environment } from '../../../environments/environment';

/**
 * Manages the Server-Sent Events (SSE) connection to the backend telemetry stream.
 * Handles authentication, retry with exponential back-off, and event parsing.
 */
@Injectable({ providedIn: 'root' })
export class TelemetryStreamService implements OnDestroy {
  private readonly auth = inject(AuthService);

  /** Live map of trainId → latest TrainPosition */
  readonly positions = signal<Map<string, TrainPosition>>(new Map());
  /** Latest feed status events keyed by feedId */
  readonly feedStatuses = signal<Map<string, FeedStatus>>(new Map());
  /** Whether the SSE connection is currently active */
  readonly connected = signal(false);

  private source: EventSource | null = null;
  private retryTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private retryDelay = 2000;
  private readonly maxRetryDelay = 30_000;

  connect(): void {
    if (this.source) return;
    this.openStream();
  }

  disconnect(): void {
    this.source?.close();
    this.source = null;
    this.connected.set(false);
    if (this.retryTimeoutId !== null) {
      clearTimeout(this.retryTimeoutId);
      this.retryTimeoutId = null;
    }
  }

  private openStream(): void {
    const token = this.auth.getToken();
    const url = token
      ? `${environment.sseUrl}?token=${encodeURIComponent(token)}`
      : environment.sseUrl;

    this.source = new EventSource(url);
    const src = this.source;

    src.addEventListener('position_update', (e: MessageEvent) => {
      this.retryDelay = 2000;
      try {
        const pos = JSON.parse(e.data) as TrainPosition;
        this.positions.update((map) => new Map(map).set(pos.trainId, pos));
      } catch {
        console.warn('[SSE] Failed to parse position_update', e.data);
      }
    });

    src.addEventListener('feed_status', (e: MessageEvent) => {
      try {
        const status = JSON.parse(e.data) as FeedStatus;
        this.feedStatuses.update((map) =>
          new Map(map).set(status.feedId, status)
        );
      } catch {
        console.warn('[SSE] Failed to parse feed_status', e.data);
      }
    });

    src.onopen = () => {
      this.connected.set(true);
    };

    src.onerror = () => {
      this.connected.set(false);
      src.close();
      this.source = null;
      this.scheduleReconnect();
    };
  }

  private scheduleReconnect(): void {
    this.retryTimeoutId = setTimeout(() => {
      this.retryDelay = Math.min(this.retryDelay * 2, this.maxRetryDelay);
      this.openStream();
    }, this.retryDelay);
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
