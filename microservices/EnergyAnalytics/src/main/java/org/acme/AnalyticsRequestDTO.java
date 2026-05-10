package org.acme;

import java.util.Collection;
import java.util.List;

public record AnalyticsRequestDTO(
    List<TelemetryEvent> events,
    List<Asset> assets
) { }
