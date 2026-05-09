package org.acme;

import java.time.LocalDateTime;

public record GridUpdateRequest (
        String address,
        String postalCode,
        LocalDateTime peakHoursStartTime,
        LocalDateTime peakHoursEndTime,
        Long maxLoad,
        Long operatorId,
        Long xCoords,
        Long yCoords
)
{}
