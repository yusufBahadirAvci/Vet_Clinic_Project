package com.vetclinic.common;

public final class LogMasker {

    private static final String UNKNOWN = "***";
    private static final int PHONE_VISIBLE_PREFIX = 6;
    private static final int PHONE_VISIBLE_SUFFIX = 2;

    private LogMasker() {
    }

    /** "ceylin@gmail.com" becomes "c*****@gmail.com". */
    public static String maskEmail(String email) {
        if (email == null) {
            return UNKNOWN;
        }
        int at = email.indexOf('@');
        if (at < 1 || at == email.length() - 1) {
            return UNKNOWN;
        }
        return email.charAt(0) + "*".repeat(at - 1) + email.substring(at);
    }

    /** "+905321234567" becomes "+90532*****67". */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() <= PHONE_VISIBLE_PREFIX + PHONE_VISIBLE_SUFFIX) {
            return UNKNOWN;
        }
        int hidden = phone.length() - PHONE_VISIBLE_PREFIX - PHONE_VISIBLE_SUFFIX;
        return phone.substring(0, PHONE_VISIBLE_PREFIX)
                + "*".repeat(hidden)
                + phone.substring(phone.length() - PHONE_VISIBLE_SUFFIX);
    }
}
