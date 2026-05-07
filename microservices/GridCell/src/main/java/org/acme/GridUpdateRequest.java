package org.acme;

import java.time.LocalDateTime;

public record GridUpdateRequest (
        String location,
        LocalDateTime peakHoursStart,
        LocalDateTime peakHoursEnd,
        Long maxLoad
)
{}
