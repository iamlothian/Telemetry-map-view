/**
 * Core telemetry models shared between frontend and backend.
 * All coordinates follow [longitude, latitude] GeoJSON convention.
 */

export type VehicleType = 'freight' | 'passenger' | 'commuter' | 'unknown';

export interface TrainPosition {
  /** Unique on-screen entity identifier (stable across updates) */
  trainId: string;
  /** Human-readable train/trip identifier per the originating feed */
  displayName: string;
  /** GeoJSON [longitude, latitude] */
  coordinates: [number, number];
  /** Speed in km/h; null if not provided by feed */
  speedKmh: number | null;
  /** Compass bearing degrees (0–360); null if not provided */
  bearing: number | null;
  /** UTC epoch ms when this position was reported by the vehicle or feed */
  reportedAtMs: number;
  /** UTC epoch ms when this update was ingested by our backend */
  receivedAtMs: number;
  vehicleType: VehicleType;
  /** Feed adapter that sourced this update */
  feedId: string;
  /** IATA/industry operator code, e.g. "AMTK" for Amtrak */
  operatorCode: string;
  /** Trip/route ID as defined by the originating feed */
  tripId: string | null;
  /** Route short name (e.g. "1", "Cardinal") */
  routeShortName: string | null;
  /** Destination stop name */
  destination: string | null;
}

export interface TelemetryEvent {
  eventType: 'position_update' | 'feed_status';
  payload: TrainPosition | FeedStatus;
}

export interface FeedStatus {
  feedId: string;
  name: string;
  healthy: boolean;
  lastSuccessMs: number | null;
  errorCount: number;
  message: string | null;
}
