package com.telemetry.feed;

import com.google.transit.realtime.GtfsRealtime;
import com.telemetry.model.FeedStatus;
import com.telemetry.model.TrainPosition;
import com.telemetry.model.VehicleType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for GTFS-Realtime VehiclePosition feeds.
 * Subclasses supply the feed URL, operator code, and tuning parameters.
 */
@Slf4j
public abstract class AbstractGtfsRtFeedAdapter implements FeedAdapter {

    protected final WebClient webClient;
    protected final Duration pollInterval;

    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final AtomicLong lastSuccessMs = new AtomicLong(0);
    private final AtomicReference<String> lastError = new AtomicReference<>();

    protected AbstractGtfsRtFeedAdapter(WebClient.Builder webClientBuilder,
                                         Duration pollInterval) {
        this.webClient = webClientBuilder.build();
        this.pollInterval = pollInterval;
    }

    /** GTFS-RT VehiclePositions feed URL */
    protected abstract String getFeedUrl();

    /** Optional static Bearer token for authenticated feeds, or null */
    protected String apiKey() {
        return null;
    }

    /** Operator code to stamp on every TrainPosition */
    protected abstract String getOperatorCode();

    @Override
    public Flux<TrainPosition> positions() {
        return Flux.interval(Duration.ZERO, pollInterval)
                .flatMap(tick -> fetchPositions())
                .onErrorContinue((err, val) -> {
                    errorCount.incrementAndGet();
                    lastError.set(err.getMessage());
                    log.error("[{}] Feed error: {}", getFeedId(), err.getMessage());
                });
    }

    private Flux<TrainPosition> fetchPositions() {
        var req = webClient.get().uri(getFeedUrl());
        if (apiKey() != null) {
            req = req.header("Authorization", "Bearer " + apiKey());
        }

        return req.retrieve()
                .bodyToMono(byte[].class)
                .flatMapMany(bytes -> {
                    Flux<TrainPosition> positions = parseGtfsRt(bytes);
                    lastSuccessMs.set(System.currentTimeMillis());
                    errorCount.set(0);
                    lastError.set(null);
                    log.debug("[{}] Fetched positions", getFeedId());
                    return positions;
                })
                .onErrorResume(err -> {
                    errorCount.incrementAndGet();
                    lastError.set(err.getMessage());
                    log.warn("[{}] Fetch failed: {}", getFeedId(), err.getMessage());
                    return Flux.empty();
                });
    }

    private Flux<TrainPosition> parseGtfsRt(byte[] bytes) {
        try {
            var feed = GtfsRealtime.FeedMessage.parseFrom(bytes);
            long receivedAt = System.currentTimeMillis();

            return Flux.fromIterable(feed.getEntityList())
                    .filter(entity -> entity.hasVehicle() && entity.getVehicle().hasPosition())
                    .map(entity -> {
                        var vehicle = entity.getVehicle();
                        var pos = vehicle.getPosition();
                        var trip = vehicle.hasTrip() ? vehicle.getTrip() : null;

                        long reportedAt = vehicle.hasTimestamp()
                                ? vehicle.getTimestamp() * 1000
                                : receivedAt;

                        return TrainPosition.builder()
                                .trainId(getFeedId() + ":" + entity.getId())
                                .displayName(vehicle.hasVehicle() ? vehicle.getVehicle().getLabel() : entity.getId())
                                .coordinates(new double[]{pos.getLongitude(), pos.getLatitude()})
                                .speedKmh(pos.hasSpeed() ? (double) (pos.getSpeed() * 3.6f) : null)
                                .bearing(pos.hasBearing() ? (double) pos.getBearing() : null)
                                .reportedAtMs(reportedAt)
                                .receivedAtMs(receivedAt)
                                .vehicleType(VehicleType.PASSENGER)
                                .feedId(getFeedId())
                                .operatorCode(getOperatorCode())
                                .tripId(trip != null && !trip.getTripId().isEmpty() ? trip.getTripId() : null)
                                .routeShortName(trip != null && !trip.getRouteId().isEmpty() ? trip.getRouteId() : null)
                                .destination(null)
                                .build();
                    });
        } catch (Exception e) {
            log.error("[{}] GTFS-RT parse error: {}", getFeedId(), e.getMessage());
            return Flux.empty();
        }
    }

    @Override
    public FeedStatus getStatus() {
        long ls = lastSuccessMs.get();
        return FeedStatus.builder()
                .feedId(getFeedId())
                .name(getDisplayName())
                .healthy(errorCount.get() == 0 && ls > 0)
                .lastSuccessMs(ls > 0 ? ls : null)
                .errorCount(errorCount.get())
                .message(lastError.get())
                .build();
    }
}
