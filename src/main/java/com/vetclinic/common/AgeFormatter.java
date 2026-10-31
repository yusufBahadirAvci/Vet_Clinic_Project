package com.vetclinic.common;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

@Component
public class AgeFormatter {

    private final Clock clock;

    public AgeFormatter(Clock clock) {
        this.clock = clock;
    }

    /** "3 years", "4 months", "2 weeks"; "less than 1 week" for newborns. */
    public String format(LocalDate birthDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date is required.");
        }
        LocalDate today = LocalDate.now(clock);
        if (birthDate.isAfter(today)) {
            throw new IllegalArgumentException("Birth date cannot be in the future.");
        }
        Period period = Period.between(birthDate, today);
        if (period.getYears() >= 1) {
            return plural(period.getYears(), "year");
        }
        if (period.getMonths() >= 1) {
            return plural(period.getMonths(), "month");
        }
        long weeks = ChronoUnit.WEEKS.between(birthDate, today);
        return weeks >= 1 ? plural(weeks, "week") : "less than 1 week";
    }

    private static String plural(long amount, String unit) {
        return amount + " " + unit + (amount == 1 ? "" : "s");
    }
}
