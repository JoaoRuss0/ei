package org.acme;

import java.util.Collection;

public record AnalyseRequestDTO(
   Collection<TelemetryEvent> events,
   Collection<GridCellData> cells
) {}
