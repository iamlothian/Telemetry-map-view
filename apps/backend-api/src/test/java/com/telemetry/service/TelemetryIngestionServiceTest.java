package com.telemetry.service;

import com.telemetry.feed.FeedAdapter;
import com.telemetry.model.FeedStatus;
import com.telemetry.model.TrainPosition;
import com.telemetry.model.TelemetryEvent;
import com.telemetry.model.VehicleType;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelemetryIngestionServiceTest {

    private static TrainPosition dummyPosition(String feedId) {
        return TrainPosition.builder()
                .trainId(feedId + ":1")
                .displayName("Test Train")
                .coordinates(new double[]{-87.65, 41.85})
                .speedKmh(100.0)
                .bearing(90.0)
                .reportedAtMs(System.currentTimeMillis())
                .receivedAtMs(System.currentTimeMillis())
                .vehicleType(VehicleType.PASSENGER)
                .feedId(feedId)
                .operatorCode("TEST")
                .tripId("trip-1")
                .routeShortName("Route 1")
                .destination("Chicago")
                .build();
    }

    @Test
    void mergesPositionsFromMultipleAdapters() {
        FeedAdapter adapter1 = mock(FeedAdapter.class);
        when(adapter1.getFeedId()).thenReturn("feed-a");
        when(adapter1.getDisplayName()).thenReturn("Feed A");
        when(adapter1.positions()).thenReturn(Flux.just(dummyPosition("feed-a")));
        when(adapter1.getStatus()).thenReturn(
                FeedStatus.builder().feedId("feed-a").name("Feed A").healthy(true)
                        .lastSuccessMs(System.currentTimeMillis()).errorCount(0).message(null).build());

        FeedAdapter adapter2 = mock(FeedAdapter.class);
        when(adapter2.getFeedId()).thenReturn("feed-b");
        when(adapter2.getDisplayName()).thenReturn("Feed B");
        when(adapter2.positions()).thenReturn(Flux.just(dummyPosition("feed-b")));
        when(adapter2.getStatus()).thenReturn(
                FeedStatus.builder().feedId("feed-b").name("Feed B").healthy(true)
                        .lastSuccessMs(System.currentTimeMillis()).errorCount(0).message(null).build());

        TelemetryIngestionService service = new TelemetryIngestionService(List.of(adapter1, adapter2));

        StepVerifier.create(service.getStream().take(2))
                .assertNext(event -> {
                    assertThat(event.getEventType()).isEqualTo("position_update");
                    assertThat(((TrainPosition) event.getPayload()).getFeedId()).isIn("feed-a", "feed-b");
                })
                .assertNext(event -> assertThat(event.getEventType()).isEqualTo("position_update"))
                .verifyComplete();
    }

    @Test
    void broadcastFeedStatusEmitsStatusEvents() {
        FeedAdapter adapter = mock(FeedAdapter.class);
        when(adapter.getFeedId()).thenReturn("feed-a");
        when(adapter.getDisplayName()).thenReturn("Feed A");
        when(adapter.positions()).thenReturn(Flux.never());
        when(adapter.getStatus()).thenReturn(
                FeedStatus.builder().feedId("feed-a").name("Feed A").healthy(false)
                        .lastSuccessMs(null).errorCount(3).message("Connection refused").build());

        TelemetryIngestionService service = new TelemetryIngestionService(List.of(adapter));
        service.broadcastFeedStatus();

        StepVerifier.create(service.getStream().next())
                .assertNext(event -> {
                    assertThat(event.getEventType()).isEqualTo("feed_status");
                    FeedStatus status = (FeedStatus) event.getPayload();
                    assertThat(status.getFeedId()).isEqualTo("feed-a");
                    assertThat(status.isHealthy()).isFalse();
                    assertThat(status.getErrorCount()).isEqualTo(3);
                })
                .verifyComplete();
    }
}
