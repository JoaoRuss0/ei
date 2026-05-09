package org.acme;

import java.time.LocalDateTime;

public record GridCellData(
        String id,
        Long maxLoad,
        Long xCoords,
        Long yCoords
) {
}
