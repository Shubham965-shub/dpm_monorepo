package com.strides.service.kpi;

import java.time.LocalDate;

/** Whether a KPI's data has stopped arriving: a whole period after its latest data has already finished with nothing reported. */
public final class StaleData {

    private StaleData() {
    }

    /** lastDataDate: any date inside the latest period that has data; asOf: today, or the range's end when that is earlier. */
    public static boolean isStale(String grain, LocalDate lastDataDate, LocalDate asOf) {
        if (lastDataDate == null) {
            return false;
        }
        LocalDate firstMissing = PeriodGaps.nextStart(grain, PeriodGaps.periodStartOf(grain, lastDataDate));
        return !PeriodGaps.nextStart(grain, firstMissing).isAfter(asOf);
    }
}
