package com.vetclinic.common;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class ContactNormalizer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern MOBILE_DIGITS = Pattern.compile("^5\\d{9}$");
    private static final int MAX_EMAIL_LENGTH = 254;

    public ContactType detectType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidContactException("Please enter an e-mail address or a phone number.");
        }
        return raw.indexOf('@') >= 0 ? ContactType.EMAIL : ContactType.PHONE;
    }

    public String normalize(String raw) {
        return detectType(raw) == ContactType.EMAIL ? normalizeEmail(raw) : normalizePhone(raw);
    }

    public String normalizeEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidContactException("Please enter an e-mail address.");
        }
        String email = raw.trim().toLowerCase(Locale.ROOT);
        if (email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidContactException("Please enter a valid e-mail address.");
        }
        return email;
    }

    /** Accepts 0532..., 532..., 90532... and +90532...; returns +90532.... */
    public String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidContactException("Please enter a phone number.");
        }
        String value = raw.replaceAll("[\\s\\-()]", "");
        if (value.startsWith("+90")) {
            value = value.substring(3);
        } else if (value.startsWith("90") && value.length() == 12) {
            value = value.substring(2);
        } else if (value.startsWith("0")) {
            value = value.substring(1);
        }
        if (!MOBILE_DIGITS.matcher(value).matches()) {
            throw new InvalidContactException(
                    "Please enter a valid mobile phone number, for example 0532 123 45 67.");
        }
        return "+90" + value;
    }
}
