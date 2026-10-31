package com.vetclinic.auth;

import com.vetclinic.user.domain.User;

/**
 * Outcome of {@link OtpService#verifyCode}. A wrong code is a result, not an exception, so the
 * attempt counter that was increased is committed (CLAUDE.md rule 12).
 */
public record VerifyResult(Status status, User user) {

    public enum Status { SUCCESS, INVALID, LOCKED }

    public static final VerifyResult INVALID = new VerifyResult(Status.INVALID, null);
    public static final VerifyResult LOCKED = new VerifyResult(Status.LOCKED, null);

    public static VerifyResult success(User user) {
        return new VerifyResult(Status.SUCCESS, user);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
