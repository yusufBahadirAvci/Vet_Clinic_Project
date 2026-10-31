package com.vetclinic.auth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

/** Generates 6-digit one-time codes (000000-999999, zero-padded) with a cryptographically secure random source. */
@Component
public class OtpCodeGenerator {

    private static final int BOUND = 1_000_000;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        return String.format(Locale.ROOT, "%06d", random.nextInt(BOUND));
    }
}
