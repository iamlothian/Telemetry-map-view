package com.telemetry.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Value;

/**
 * Envelope for SSE events sent to the frontend.
 * eventType determines how the payload is typed.
 */
@Value
@Builder
public class TelemetryEvent {
    /** Matches the SSE event name so the frontend can dispatch correctly */
    String eventType;
    Object payload;
}
