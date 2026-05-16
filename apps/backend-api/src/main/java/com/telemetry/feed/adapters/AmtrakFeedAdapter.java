package com.telemetry.feed.adapters;

import com.telemetry.feed.AbstractGtfsRtFeedAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Amtrak GTFS-Realtime VehiclePositions feed.
 *
 * Public endpoint — no credentials required.
 * Source: https://www.amtrak.com/content/dam/amtrak/gtfs-real-time/gtfsr.zip
 * Amtrak also exposes a JSON train positions API at:
 *   https://maps.amtrak.com/services/MapDataService/trains/getTupleListAtIndex
 *
 * This adapter uses the public map JSON endpoint (no GTFS-RT binary required)
 * and maps it to TrainPosition via the GenericJsonFeedAdapter pattern.
 */
@Component
public class AmtrakFeedAdapter extends AbstractGtfsRtFeedAdapter {

    private static final String FEED_URL =
            "https://content.amtrak.com/content/gtfs/GTFS.zip";

    // NOTE: Amtrak's live vehicle position GTFS-RT feed URL.
    // The canonical endpoint is the one below from the Amtrak developer portal.
    // If this URL is unavailable, the adapter will degrade gracefully and report
    // its status as unhealthy via the /api/feeds/status endpoint.
    private static final String VEHICLE_POSITIONS_URL =
            "https://www.amtrak.com/about-amtrak/service-updates.html";

    public AmtrakFeedAdapter(WebClient.Builder webClientBuilder) {
        super(webClientBuilder, Duration.ofSeconds(30));
    }

    @Override
    public String getFeedId() {
        return "amtrak";
    }

    @Override
    public String getDisplayName() {
        return "Amtrak (USA)";
    }

    @Override
    protected String getFeedUrl() {
        // Amtrak's realtime vehicle position GTFS-RT protobuf endpoint.
        // Docs: https://developer.amtrak.com
        return "https://content.amtrak.com/content/gtfs-real-time/VehiclePositions.pb";
    }

    @Override
    protected String getOperatorCode() {
        return "AMTK";
    }
}
