package org.acme;

import java.util.Collection;

public record AnalyticsRequestDTO(
    Collection<TelemetryEvent> events,
    Collection<Asset> assets
) { }
