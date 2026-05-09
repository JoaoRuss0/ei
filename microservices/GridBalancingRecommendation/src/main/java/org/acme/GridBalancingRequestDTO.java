package org.acme;

import java.util.Collection;

public record GridBalancingRequestDTO(
   Collection<TelemetryEvent> events,
   Collection<GridCellData> cells
) {}
