package org.acme;

import java.time.LocalDateTime;

public record GridUpdateRequest (
        String address,
        String postalCode,
        LocalDateTime peakHoursStart,
        LocalDateTime peakHoursEnd,
        Long maxLoad,
        Long operatorId
)
{}
