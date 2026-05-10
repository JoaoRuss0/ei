package org.acme;

import java.util.Collection;
import java.util.List;

public record GridBalancingRequestDTO(
   List<TelemetryEvent> events,
   List<GridCellData> cells
) {}
