package org.acme;

import java.util.Collection;
import java.util.List;

public record EvaluateRequestDTO(
   long prosumer_id,
   List<TelemetryEvent> events,
   List<GridCellData> cells
) {}
