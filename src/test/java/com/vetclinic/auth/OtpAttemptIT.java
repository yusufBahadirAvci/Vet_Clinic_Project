package com.vetclinic.auth;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.auth.domain.OtpCode;
import com.vetclinic.auth.repository.OtpCodeRepository;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.vetclinic.support.TestData.owner;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Not transactional on purpose: with a real commit per service call this proves that the attempt counter of a
 * wrong code survives (CLAUDE.md rule 12). A mock repository cannot catch a rollback bug.
 */
class OtpAttemptIT extends AbstractIntegrationTest {

    private static final String PHONE = "+905320000009";
    private static final String CODE = "135790";

    @Autowired
    private OtpService otpService;
    @Autowired
    private OtpCodeRepository otpCodeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM otp_codes");
        jdbcTemplate.update("DELETE FROM sms_outbox");
        jdbcTemplate.update("DELETE FROM users WHERE phone = ?", PHONE);
    }

    @Test
    void fiveWrongAttemptsAreCommittedAndThenEvenTheCorrectCodeIsRejected() {
        User user = owner("Attempt Owner", null);
        user.setPhone(PHONE);
        userRepository.save(user);
        otpService.requestCode(PHONE);
        OtpCode otp = otpCodeRepository.findTopByDestinationOrderByCreatedAtDesc(PHONE).orElseThrow();
        otp.setCodeHash(passwordEncoder.encode(CODE));
        otpCodeRepository.save(otp);

        for (int i = 1; i <= 4; i++) {
            assertThat(otpService.verifyCode(PHONE, "000000")).isEqualTo(VerifyResult.INVALID);
            assertThat(storedAttemptCount()).isEqualTo(i);
        }
        assertThat(otpService.verifyCode(PHONE, "000000")).isEqualTo(VerifyResult.LOCKED);

        assertThat(storedAttemptCount()).isEqualTo(5);
        assertThat(otpService.verifyCode(PHONE, CODE)).isEqualTo(VerifyResult.LOCKED);
        assertThat(jdbcTemplate.queryForObject("SELECT used_at FROM otp_codes", java.sql.Timestamp.class)).isNull();
    }

    private int storedAttemptCount() {
        return jdbcTemplate.queryForObject("SELECT attempt_count FROM otp_codes", Integer.class);
    }
}
