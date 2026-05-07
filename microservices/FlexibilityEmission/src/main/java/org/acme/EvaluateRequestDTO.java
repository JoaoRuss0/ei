package org.acme;

import java.util.Collection;

public record EvaluateRequestDTO(
   long prosumer_id,
   Collection<TelemetryEvent> events,
   Collection<GridCellData> cells
) {}
