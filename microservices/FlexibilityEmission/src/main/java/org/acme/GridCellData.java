package org.acme;

import java.time.LocalDateTime;

public record GridCellData(
        String id,
        LocalDateTime peakHoursStartTime,
        LocalDateTime peakHoursEndTime
) {
}
