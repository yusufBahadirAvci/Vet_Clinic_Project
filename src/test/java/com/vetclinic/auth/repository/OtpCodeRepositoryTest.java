package com.vetclinic.auth.repository;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.auth.domain.OtpCode;
import com.vetclinic.notification.domain.Channel;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static com.vetclinic.support.TestData.owner;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class OtpCodeRepositoryTest extends AbstractIntegrationTest {

    private static final String DESTINATION = "owner@example.com";
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-31T12:00:00+03:00");

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(owner("Owner", DESTINATION));
    }

    @Test
    void findAllByUserIdAndUsedAtIsNullReturnsOnlyUnusedCodes() {
        OtpCode unused = save(DESTINATION);
        OtpCode used = save(DESTINATION);
        used.setUsedAt(NOW);
        otpCodeRepository.flush();

        assertThat(otpCodeRepository.findAllByUserIdAndUsedAtIsNull(user.getId())).containsExactly(unused);
    }

    @Test
    void findTopByDestinationOrderByCreatedAtDescReturnsTheLatestCode() {
        OtpCode older = save(DESTINATION);
        OtpCode latest = save(DESTINATION);
        save("+905321234567");
        otpCodeRepository.flush();
        setCreatedAt(older, NOW.minusMinutes(10));
        setCreatedAt(latest, NOW.minusMinutes(1));

        assertThat(otpCodeRepository.findTopByDestinationOrderByCreatedAtDesc(DESTINATION))
                .get().extracting(OtpCode::getId).isEqualTo(latest.getId());
        assertThat(otpCodeRepository.findTopByDestinationOrderByCreatedAtDesc("nobody@example.com")).isEmpty();
    }

    @Test
    void countByDestinationAndCreatedAtAfterCountsOnlyRecentCodesForThatDestination() {
        OtpCode old = save(DESTINATION);
        OtpCode recent1 = save(DESTINATION);
        OtpCode recent2 = save(DESTINATION);
        OtpCode otherDestination = save("+905321234567");
        otpCodeRepository.flush();
        setCreatedAt(old, NOW.minusMinutes(20));
        setCreatedAt(recent1, NOW.minusMinutes(5));
        setCreatedAt(recent2, NOW.minusMinutes(1));
        setCreatedAt(otherDestination, NOW.minusMinutes(1));

        assertThat(otpCodeRepository.countByDestinationAndCreatedAtAfter(DESTINATION, NOW.minusMinutes(15))).isEqualTo(2);
    }

    private OtpCode save(String destination) {
        OtpCode code = new OtpCode();
        code.setUser(user);
        code.setChannel(destination.startsWith("+") ? Channel.SMS : Channel.EMAIL);
        code.setDestination(destination);
        code.setCodeHash("$2a$10$hashhashhashhashhashhu");
        code.setExpiresAt(NOW.plusMinutes(5));
        return otpCodeRepository.save(code);
    }

    /** created_at is DEFAULT now(), identical inside one test transaction; set distinct values explicitly. */
    private void setCreatedAt(OtpCode code, OffsetDateTime createdAt) {
        jdbcTemplate.update("UPDATE otp_codes SET created_at = ? WHERE id = ?", createdAt, code.getId());
    }
}
