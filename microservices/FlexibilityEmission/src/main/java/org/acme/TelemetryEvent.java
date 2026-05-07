package org.acme;

import java.time.LocalDateTime;

public record TelemetryEvent(
        Long id,
        LocalDateTime timeStamp,
        Long asset_id,
        String asset_type,
        String grid_cell_id,
        Float State_of_Charge,
        Float Available_Energy,
        Float Current_Output,
        Float Max_Capacity,
        Float State_of_Health,
        String Status,
        Float Current_Generation,
        Float Daily_Total,
        Float Grid_Voltage,
        Float Frequency,
        String Plug_Status,
        Float Charging_Rate,
        Float Session_Energy,
        Float EV_SoC
) {
}
