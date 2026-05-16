package com.telemetry.model;

import lombok.Builder;
import lombok.Value;

/**
 * Normalized telemetry position event emitted to the frontend.
 * All feeds produce this model; the frontend consumes it via SSE.
 */
@Value
@Builder
public class TrainPosition {
    /** Stable unique ID for this train on-screen */
    String trainId;
    /** Human-readable name from the originating feed */
    String displayName;
    /** GeoJSON [longitude, latitude] */
    double[] coordinates;
    /** km/h – null if not reported */
    Double speedKmh;
    /** Compass degrees 0-360 – null if not reported */
    Double bearing;
    /** UTC ms: when the vehicle reported this position */
    long reportedAtMs;
    /** UTC ms: when we ingested this position */
    long receivedAtMs;
    VehicleType vehicleType;
    /** Which feed adapter produced this update */
    String feedId;
    /** IATA/industry operator code e.g. AMTK */
    String operatorCode;
    String tripId;
    String routeShortName;
    String destination;
}
