package com.telemetry.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetry.model.TelemetryEvent;
import com.telemetry.service.TelemetryIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.UUID;

/**
 * SSE endpoint that streams live telemetry events to connected Angular frontend clients.
 *
 * Event types emitted:
 *   - position_update — {@link com.telemetry.model.TrainPosition} payload
 *   - feed_status    — {@link com.telemetry.model.FeedStatus} payload
 *   - heartbeat      — empty keepalive every 20s (prevents proxy/load-balancer timeouts)
 */
@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
@Slf4j
public class TelemetryController {

    private final TelemetryIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream() {
        String clientId = UUID.randomUUID().toString().substring(0, 8);
        log.info("SSE client connected: {}", clientId);

        Flux<ServerSentEvent<String>> events = ingestionService.getStream()
                .map(event -> ServerSentEvent.<String>builder()
                        .event(event.getEventType())
                        .data(serialize(event.getPayload()))
                        .build())
                .onErrorContinue((err, val) ->
                        log.warn("[SSE] Serialization error for client {}: {}", clientId, err.getMessage()));

        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(20))
                .map(tick -> ServerSentEvent.<String>builder()
                        .event("heartbeat")
                        .data("")
                        .build());

        return Flux.merge(events, heartbeat)
                .doOnCancel(() -> log.info("SSE client disconnected: {}", clientId))
                .doOnTerminate(() -> log.info("SSE stream terminated for client: {}", clientId));
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload: {}", e.getMessage());
            return "{}";
        }
    }
}
