package com.vetclinic.notification.domain;

/**
 * Delivery channel of a notification or OTP code.
 * Shared by the auth package (OtpCode) and the notification package (see docs/decisions.md, decision 11).
 */
public enum Channel {
    EMAIL,
    SMS
}
