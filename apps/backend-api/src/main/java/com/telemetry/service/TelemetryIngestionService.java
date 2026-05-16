package com.telemetry.service;

import com.telemetry.feed.FeedAdapter;
import com.telemetry.model.FeedStatus;
import com.telemetry.model.TelemetryEvent;
import com.telemetry.model.TrainPosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

/**
 * Coordinates all registered {@link FeedAdapter}s into a single merged telemetry stream.
 *
 * <ul>
 *   <li>A hot {@link Sinks.Many} multicast sink allows multiple SSE clients to subscribe.</li>
 *   <li>Position updates are ingested from each adapter and emitted as {@link TelemetryEvent}s.</li>
 *   <li>Feed status is broadcast on a schedule.</li>
 * </ul>
 */
@Service
@Slf4j
public class TelemetryIngestionService {

    private final Sinks.Many<TelemetryEvent> sink;
    private final Flux<TelemetryEvent> sharedStream;
    private final List<FeedAdapter> adapters;

    public TelemetryIngestionService(List<FeedAdapter> adapters) {
        this.adapters = adapters;

        this.sink = Sinks.many().multicast().onBackpressureBuffer(1024);
        this.sharedStream = sink.asFlux().share();

        // Merge all adapter position streams and push to the hot sink
        Flux.merge(adapters.stream()
                        .map(adapter -> adapter.positions()
                                .map(pos -> TelemetryEvent.builder()
                                        .eventType("position_update")
                                        .payload(pos)
                                        .build())
                                .onErrorContinue((err, val) ->
                                        log.error("[ingestion] Adapter {} error: {}",
                                                adapter.getFeedId(), err.getMessage())))
                        .toList())
                .subscribe(
                        event -> sink.tryEmitNext(event),
                        err -> log.error("[ingestion] Unrecoverable stream error", err)
                );

        log.info("TelemetryIngestionService started with {} adapters: {}",
                adapters.size(),
                adapters.stream().map(FeedAdapter::getFeedId).toList());
    }

    /** Returns the hot shared stream of all telemetry events for SSE clients. */
    public Flux<TelemetryEvent> getStream() {
        return sharedStream;
    }

    /** Broadcast feed health status to all connected SSE subscribers every 10 seconds. */
    @Scheduled(fixedDelay = 10_000)
    public void broadcastFeedStatus() {
        adapters.forEach(adapter -> {
            FeedStatus status = adapter.getStatus();
            TelemetryEvent event = TelemetryEvent.builder()
                    .eventType("feed_status")
                    .payload(status)
                    .build();
            sink.tryEmitNext(event);
        });
    }
}
