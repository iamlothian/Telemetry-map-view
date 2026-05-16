package com.telemetry.controller;

import com.telemetry.feed.FeedAdapter;
import com.telemetry.model.FeedStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public endpoint that returns the health status of all registered feed adapters. */
@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedStatusController {

    private final List<FeedAdapter> adapters;

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FeedStatus> status() {
        return adapters.stream().map(FeedAdapter::getStatus).toList();
    }
}
