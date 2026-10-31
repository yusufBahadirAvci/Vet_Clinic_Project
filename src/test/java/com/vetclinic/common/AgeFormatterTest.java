package com.vetclinic.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class AgeFormatterTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    private final Clock clock = Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);
    private final AgeFormatter formatter = new AgeFormatter(clock);

    @Test
    void formatsYears() {
        assertThat(formatter.format(TODAY.minusYears(3).minusMonths(2))).isEqualTo("3 years");
        assertThat(formatter.format(TODAY.minusYears(1))).isEqualTo("1 year");
    }

    @Test
    void formatsMonths() {
        assertThat(formatter.format(TODAY.minusMonths(4).minusDays(3))).isEqualTo("4 months");
        assertThat(formatter.format(TODAY.minusMonths(1))).isEqualTo("1 month");
        assertThat(formatter.format(TODAY.minusYears(1).plusDays(1))).isEqualTo("11 months");
    }

    @Test
    void formatsWeeks() {
        assertThat(formatter.format(TODAY.minusWeeks(2).minusDays(3))).isEqualTo("2 weeks");
        assertThat(formatter.format(TODAY.minusDays(7))).isEqualTo("1 week");
        assertThat(formatter.format(TODAY.minusMonths(1).plusDays(1))).isEqualTo("4 weeks");
    }

    @Test
    void formatsNewborn() {
        assertThat(formatter.format(TODAY)).isEqualTo("less than 1 week");
        assertThat(formatter.format(TODAY.minusDays(6))).isEqualTo("less than 1 week");
    }

    @Test
    void rejectsFutureDate() {
        assertThatThrownBy(() -> formatter.format(TODAY.plusDays(1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> formatter.format(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void usesClinicTimeZone() {
        // 22:30 UTC on Oct 30 is already Oct 31 in Istanbul (UTC+3)
        Clock lateUtc = Clock.fixed(Instant.parse("2026-10-30T22:30:00Z"), ZONE);
        assertThat(new AgeFormatter(lateUtc).format(LocalDate.of(2026, 10, 31))).isEqualTo("less than 1 week");
    }
}
