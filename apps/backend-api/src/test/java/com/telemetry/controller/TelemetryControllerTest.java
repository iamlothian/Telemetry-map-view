package com.telemetry.controller;

import com.telemetry.model.FeedStatus;
import com.telemetry.model.TelemetryEvent;
import com.telemetry.model.TrainPosition;
import com.telemetry.model.VehicleType;
import com.telemetry.service.TelemetryIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.mockito.Mockito.when;

@WebFluxTest(TelemetryController.class)
class TelemetryControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private TelemetryIngestionService ingestionService;

    @Test
    @WithMockUser
    void streamReturnsTextEventStream() {
        TrainPosition pos = TrainPosition.builder()
                .trainId("amtrak:1")
                .displayName("Test Train")
                .coordinates(new double[]{-87.65, 41.85})
                .feedId("amtrak")
                .operatorCode("AMTK")
                .vehicleType(VehicleType.PASSENGER)
                .reportedAtMs(System.currentTimeMillis())
                .receivedAtMs(System.currentTimeMillis())
                .build();

        TelemetryEvent event = TelemetryEvent.builder()
                .eventType("position_update")
                .payload(pos)
                .build();

        when(ingestionService.getStream()).thenReturn(Flux.just(event));

        webTestClient.get()
                .uri("/api/telemetry/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
    }

    @Test
    void streamRequiresAuthentication() {
        webTestClient.get()
                .uri("/api/telemetry/stream")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
