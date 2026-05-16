package com.telemetry.feed;

import com.telemetry.model.FeedStatus;
import com.telemetry.model.TrainPosition;
import reactor.core.publisher.Flux;

/**
 * Contract for all telemetry feed adapters.
 * Implement this interface to add new feeds — the ingestion pipeline will pick them up automatically.
 */
public interface FeedAdapter {

    /**
     * Unique identifier for this feed (used in SSE events and feed status).
     * Must be stable across restarts.
     */
    String getFeedId();

    /** Human-readable feed name for display in the UI. */
    String getDisplayName();

    /**
     * Returns a {@link Flux} of {@link TrainPosition} events.
     * Implementations should handle their own polling/streaming and emit continuously.
     * This Flux must be cold — re-subscribing fully restarts the feed.
     */
    Flux<TrainPosition> positions();

    /**
     * Returns the current health status of this feed.
     * Called periodically by the ingestion coordinator to broadcast status SSE events.
     */
    FeedStatus getStatus();
}
