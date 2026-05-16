package com.telemetry.feed.adapters;

import com.telemetry.feed.AbstractGtfsRtFeedAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Metrolinx (GO Transit) GTFS-Realtime VehiclePositions feed.
 *
 * Metrolinx publishes public GTFS-RT data for GO Transit commuter rail in the
 * Greater Toronto / Hamilton area.
 *
 * Public endpoint — no credentials required.
 * Source: https://www.metrolinx.com/en/aboutmetrolinx/open-data.aspx
 */
@Component
public class MetrolinxFeedAdapter extends AbstractGtfsRtFeedAdapter {

    public MetrolinxFeedAdapter(WebClient.Builder webClientBuilder) {
        super(webClientBuilder, Duration.ofSeconds(15));
    }

    @Override
    public String getFeedId() {
        return "metrolinx-go";
    }

    @Override
    public String getDisplayName() {
        return "GO Transit / Metrolinx (Canada)";
    }

    @Override
    protected String getFeedUrl() {
        // Metrolinx public GTFS-RT VehiclePositions endpoint
        return "https://api.openmetrolinx.com/OpenDataAPI/api/V1/Gtfs.proto/File/VehiclePosition";
    }

    @Override
    protected String getOperatorCode() {
        return "GOTO";
    }
}
