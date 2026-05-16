package com.telemetry.model;

import lombok.Builder;
import lombok.Value;

/** Feed health status broadcast to the frontend via SSE */
@Value
@Builder
public class FeedStatus {
    String feedId;
    String name;
    boolean healthy;
    Long lastSuccessMs;
    int errorCount;
    String message;
}
