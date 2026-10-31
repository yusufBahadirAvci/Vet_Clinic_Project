package com.vetclinic.vaccination;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

@Component
public class VaccinationStatusCalculator {

    static final int DUE_SOON_DAYS = 30;

    private final Clock clock;

    public VaccinationStatusCalculator(Clock clock) {
        this.clock = clock;
    }

    public VaccinationStatus statusOf(LocalDate nextDueDate) {
        if (nextDueDate == null) {
            return VaccinationStatus.NONE;
        }
        LocalDate today = LocalDate.now(clock);
        if (nextDueDate.isBefore(today)) {
            return VaccinationStatus.OVERDUE;
        }
        if (!nextDueDate.isAfter(today.plusDays(DUE_SOON_DAYS))) {
            return VaccinationStatus.DUE_SOON;
        }
        return VaccinationStatus.OK;
    }
}
