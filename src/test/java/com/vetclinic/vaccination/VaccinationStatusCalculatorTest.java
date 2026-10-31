package com.vetclinic.vaccination;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class VaccinationStatusCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    private final VaccinationStatusCalculator calculator =
            new VaccinationStatusCalculator(Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE));

    @Test
    void nullIsNone() {
        assertThat(calculator.statusOf(null)).isEqualTo(VaccinationStatus.NONE);
    }

    @Test
    void pastDateIsOverdue() {
        assertThat(calculator.statusOf(TODAY.minusDays(1))).isEqualTo(VaccinationStatus.OVERDUE);
        assertThat(calculator.statusOf(TODAY.minusYears(1))).isEqualTo(VaccinationStatus.OVERDUE);
    }

    @Test
    void todayThroughThirtyDaysIsDueSoon() {
        assertThat(calculator.statusOf(TODAY)).isEqualTo(VaccinationStatus.DUE_SOON);
        assertThat(calculator.statusOf(TODAY.plusDays(7))).isEqualTo(VaccinationStatus.DUE_SOON);
        assertThat(calculator.statusOf(TODAY.plusDays(30))).isEqualTo(VaccinationStatus.DUE_SOON);
    }

    @Test
    void laterThanThirtyDaysIsOk() {
        assertThat(calculator.statusOf(TODAY.plusDays(31))).isEqualTo(VaccinationStatus.OK);
        assertThat(calculator.statusOf(TODAY.plusYears(1))).isEqualTo(VaccinationStatus.OK);
    }
}
