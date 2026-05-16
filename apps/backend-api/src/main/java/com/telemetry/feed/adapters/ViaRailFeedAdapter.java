package com.telemetry.feed.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetry.feed.FeedAdapter;
import com.telemetry.model.FeedStatus;
import com.telemetry.model.TrainPosition;
import com.telemetry.model.VehicleType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * VIA Rail Canada GTFS-Realtime-compatible feed adapter.
 *
 * VIA Rail provides real-time train status information through their Open Data portal.
 * This adapter polls VIA Rail's public train status JSON API and normalizes the data
 * into TrainPosition events.
 *
 * Source: https://www.viarail.ca/en/about-via-rail/our-company/open-data
 * No credentials required.
 */
@Component
@Slf4j
public class ViaRailFeedAdapter implements FeedAdapter {

    private static final String FEED_ID = "via-rail";
    private static final String DISPLAY_NAME = "VIA Rail (Canada)";
    // VIA Rail's public train positions JSON feed
    private static final String FEED_URL =
            "https://tsimobile.viarail.ca/data/allData.json";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final AtomicLong lastSuccessMs = new AtomicLong(0);
    private final AtomicReference<String> lastError = new AtomicReference<>();

    public ViaRailFeedAdapter(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String getFeedId() {
        return FEED_ID;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public Flux<TrainPosition> positions() {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(30))
                .flatMap(tick -> fetchPositions())
                .onErrorContinue((err, val) -> {
                    errorCount.incrementAndGet();
                    lastError.set(err.getMessage());
                    log.error("[{}] Feed error: {}", FEED_ID, err.getMessage());
                });
    }

    private Flux<TrainPosition> fetchPositions() {
        return webClient.get()
                .uri(FEED_URL)
                .retrieve()
                .bodyToMono(byte[].class)
                .flatMapMany(bytes -> {
                    try {
                        JsonNode root = objectMapper.readTree(bytes);
                        long receivedAt = System.currentTimeMillis();
                        lastSuccessMs.set(receivedAt);
                        errorCount.set(0);

                        return Flux.fromIterable(root::elements)
                                .filter(train -> train.has("lat") && train.has("lng"))
                                .map(train -> mapToPosition(train, receivedAt));
                    } catch (Exception e) {
                        lastError.set(e.getMessage());
                        errorCount.incrementAndGet();
                        log.warn("[{}] Parse error: {}", FEED_ID, e.getMessage());
                        return Flux.empty();
                    }
                })
                .onErrorResume(err -> {
                    lastError.set(err.getMessage());
                    errorCount.incrementAndGet();
                    log.warn("[{}] Fetch error: {}", FEED_ID, err.getMessage());
                    return Flux.empty();
                });
    }

    private TrainPosition mapToPosition(JsonNode train, long receivedAt) {
        String id = train.path("number").asText("unknown");
        double lat = train.path("lat").asDouble();
        double lng = train.path("lng").asDouble();
        String destination = train.path("destination").asText(null);
        String routeName = train.path("trainName").asText(null);

        return TrainPosition.builder()
                .trainId(FEED_ID + ":" + id)
                .displayName(routeName != null ? routeName + " #" + id : "Train #" + id)
                .coordinates(new double[]{lng, lat})
                .speedKmh(null)
                .bearing(null)
                .reportedAtMs(receivedAt)
                .receivedAtMs(receivedAt)
                .vehicleType(VehicleType.PASSENGER)
                .feedId(FEED_ID)
                .operatorCode("VIAR")
                .tripId(id)
                .routeShortName(routeName)
                .destination(destination)
                .build();
    }

    @Override
    public FeedStatus getStatus() {
        long ls = lastSuccessMs.get();
        return FeedStatus.builder()
                .feedId(FEED_ID)
                .name(DISPLAY_NAME)
                .healthy(errorCount.get() == 0 && ls > 0)
                .lastSuccessMs(ls > 0 ? ls : null)
                .errorCount(errorCount.get())
                .message(lastError.get())
                .build();
    }
}
