import { Injectable, signal } from '@angular/core';
import mapboxgl, { Map, LngLatLike, Marker } from 'mapbox-gl';
import { environment } from '../../../environments/environment';
import { TrainPosition } from 'shared-models';

/**
 * Manages the Mapbox GL map instance.
 * Handles marker creation, update, and cleanup for live train positions.
 */
@Injectable({ providedIn: 'root' })
export class MapboxService {
  /** Whether the map has fully loaded its base style */
  readonly mapReady = signal(false);

  private map: Map | null = null;
  /** Track Mapbox markers by trainId for efficient O(1) updates */
  private readonly markers = new Map<string, Marker>();

  initMap(container: HTMLElement, initialCenter: LngLatLike = [-98, 45]): Map {
    mapboxgl.accessToken = environment.mapboxToken;
    this.map = new mapboxgl.Map({
      container,
      style: 'mapbox://styles/mapbox/dark-v11',
      center: initialCenter,
      zoom: 4,
    });

    this.map.on('load', () => this.mapReady.set(true));
    return this.map;
  }

  /**
   * Upsert a train marker on the map.
   * Creates on first encounter, updates position/rotation on subsequent calls.
   */
  upsertTrainMarker(position: TrainPosition): void {
    if (!this.map) return;

    const existing = this.markers.get(position.trainId);
    const lngLat: LngLatLike = position.coordinates as [number, number];

    if (existing) {
      existing.setLngLat(lngLat);
      const el = existing.getElement();
      if (position.bearing !== null) {
        el.style.transform += ` rotate(${position.bearing}deg)`;
      }
      return;
    }

    const el = this.createMarkerElement(position);
    const marker = new mapboxgl.Marker({ element: el })
      .setLngLat(lngLat)
      .setPopup(this.createPopup(position))
      .addTo(this.map);

    this.markers.set(position.trainId, marker);
  }

  removeTrainMarker(trainId: string): void {
    const marker = this.markers.get(trainId);
    marker?.remove();
    this.markers.delete(trainId);
  }

  clearAllMarkers(): void {
    this.markers.forEach((m) => m.remove());
    this.markers.clear();
  }

  destroy(): void {
    this.clearAllMarkers();
    this.map?.remove();
    this.map = null;
    this.mapReady.set(false);
  }

  private createMarkerElement(pos: TrainPosition): HTMLElement {
    const el = document.createElement('div');
    el.className = 'train-marker';
    el.setAttribute('data-train-id', pos.trainId);
    el.setAttribute('data-feed-id', pos.feedId);
    el.title = `${pos.displayName} (${pos.operatorCode})`;
    if (pos.bearing !== null) {
      el.style.transform = `rotate(${pos.bearing}deg)`;
    }
    return el;
  }

  private createPopup(pos: TrainPosition): mapboxgl.Popup {
    const age = Math.round((Date.now() - pos.reportedAtMs) / 1000);
    return new mapboxgl.Popup({ offset: 20 }).setHTML(`
      <div class="train-popup">
        <strong>${pos.displayName}</strong>
        <div class="operator">${pos.operatorCode}</div>
        ${pos.routeShortName ? `<div>Route: ${pos.routeShortName}</div>` : ''}
        ${pos.destination ? `<div>→ ${pos.destination}</div>` : ''}
        ${pos.speedKmh !== null ? `<div>${pos.speedKmh} km/h</div>` : ''}
        <div class="age">${age}s ago</div>
        <div class="feed">Feed: ${pos.feedId}</div>
      </div>
    `);
  }
}
