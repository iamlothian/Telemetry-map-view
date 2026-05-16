import {
  Component,
  effect,
  ElementRef,
  inject,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { MapboxService } from '../../core/map/mapbox.service';
import { TelemetryStreamService } from '../../core/telemetry/telemetry-stream.service';
import { AuthService } from '../../core/auth/auth.service';

/**
 * Full-screen map view that renders live train positions.
 * Connects the telemetry SSE stream and delegates rendering to MapboxService.
 */
@Component({
  selector: 'app-map-view',
  template: `
    <div class="map-container" #mapContainer></div>
    @if (!mapbox.mapReady()) {
      <div class="map-loading">Loading map…</div>
    }
    @if (!stream.connected()) {
      <div class="stream-status disconnected" role="status" aria-live="polite">
        Reconnecting to live feed…
      </div>
    }
  `,
  styleUrl: './map-view.component.scss',
})
export class MapViewComponent implements OnInit, OnDestroy {
  protected readonly mapbox = inject(MapboxService);
  protected readonly stream = inject(TelemetryStreamService);
  private readonly auth = inject(AuthService);

  @ViewChild('mapContainer', { static: true })
  private mapContainer!: ElementRef<HTMLElement>;

  constructor() {
    // React to position updates from the signal in a reactive effect
    effect(() => {
      const positions = this.stream.positions();
      positions.forEach((pos) => this.mapbox.upsertTrainMarker(pos));
    });
  }

  ngOnInit(): void {
    this.mapbox.initMap(this.mapContainer.nativeElement);
    if (this.auth.state().authenticated) {
      this.stream.connect();
    }
  }

  ngOnDestroy(): void {
    this.stream.disconnect();
    this.mapbox.destroy();
  }
}
