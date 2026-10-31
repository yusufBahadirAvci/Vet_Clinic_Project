package com.vetclinic.auth;

import com.vetclinic.auth.domain.OtpCode;
import com.vetclinic.auth.repository.OtpCodeRepository;
import com.vetclinic.common.ContactNormalizer;
import com.vetclinic.common.InvalidContactException;
import com.vetclinic.notification.NotificationService;
import com.vetclinic.notification.domain.Channel;
import com.vetclinic.user.domain.Role;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String EMAIL = "ayse@example.com";
    private static final String PHONE = "+905321234567";
    private static final String CODE = "123456";
    private static final String OTHER_CODE = "654321";

    /** A clock the test can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-31T09:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Mock
    private OtpCodeRepository otpCodeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OtpCodeGenerator codeGenerator;
    @Mock
    private NotificationService notificationService;

    private final MutableClock clock = new MutableClock();
    /** Stands in for the otp_codes table; newest last. */
    private final List<OtpCode> stored = new ArrayList<>();
    private OtpService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new OtpService(otpCodeRepository, userRepository, new ContactNormalizer(), codeGenerator,
                new BCryptPasswordEncoder(), notificationService, clock);

        user = new User();
        user.setId(7L);
        user.setFullName("Ayse");
        user.setEmail(EMAIL);
        user.setPhone(PHONE);
        user.setRole(Role.OWNER);
        lenient().when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        lenient().when(userRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));

        lenient().when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(invocation -> {
            OtpCode otp = invocation.getArgument(0);
            if (!stored.contains(otp)) {
                stored.add(otp);
            }
            return otp;
        });
        lenient().when(otpCodeRepository.findAllByUserIdAndUsedAtIsNull(7L))
                .thenAnswer(invocation -> stored.stream().filter(o -> o.getUsedAt() == null).toList());
        lenient().when(otpCodeRepository.findTopByDestinationOrderByCreatedAtDesc(any())).thenAnswer(invocation ->
                stored.stream().filter(o -> o.getDestination().equals(invocation.getArgument(0)))
                        .reduce((first, second) -> second));
        lenient().when(codeGenerator.generate()).thenReturn(CODE);
    }

    @Test
    void correctCodeReturnsTheUser() {
        service.requestCode(EMAIL);

        VerifyResult result = service.verifyCode(EMAIL, CODE);

        assertThat(result.status()).isEqualTo(VerifyResult.Status.SUCCESS);
        assertThat(result.user()).isSameAs(user);
        assertThat(stored.get(0).getUsedAt()).isNotNull();
    }

    @Test
    void rawContactIsNormalizedOnBothSides() {
        service.requestCode("  Ayse@Example.COM ");

        assertThat(service.verifyCode("AYSE@example.com", CODE).isSuccess()).isTrue();
    }

    @Test
    void codeIsRejectedAfterFiveMinutesAndOneSecond() {
        service.requestCode(EMAIL);
        clock.advance(Duration.ofMinutes(5).plusSeconds(1));

        assertThat(service.verifyCode(EMAIL, CODE)).isEqualTo(VerifyResult.INVALID);
    }

    @Test
    void codeIsStillAcceptedJustBeforeItExpires() {
        service.requestCode(EMAIL);
        clock.advance(Duration.ofMinutes(4).plusSeconds(59));

        assertThat(service.verifyCode(EMAIL, CODE).isSuccess()).isTrue();
    }

    @Test
    void usedCodeCannotBeUsedTwice() {
        service.requestCode(EMAIL);
        assertThat(service.verifyCode(EMAIL, CODE).isSuccess()).isTrue();

        assertThat(service.verifyCode(EMAIL, CODE)).isEqualTo(VerifyResult.INVALID);
    }

    @Test
    void wrongCodeIncreasesTheAttemptCountAndReturnsInvalid() {
        service.requestCode(EMAIL);

        assertThat(service.verifyCode(EMAIL, "000000")).isEqualTo(VerifyResult.INVALID);

        assertThat(stored.get(0).getAttemptCount()).isEqualTo(1);
        assertThat(stored.get(0).getUsedAt()).isNull();
    }

    @Test
    void fiveWrongAttemptsLockTheCodeEvenForTheCorrectCode() {
        service.requestCode(EMAIL);

        for (int i = 1; i <= 4; i++) {
            assertThat(service.verifyCode(EMAIL, "000000")).isEqualTo(VerifyResult.INVALID);
        }
        assertThat(service.verifyCode(EMAIL, "000000")).isEqualTo(VerifyResult.LOCKED);

        assertThat(stored.get(0).getAttemptCount()).isEqualTo(5);
        assertThat(service.verifyCode(EMAIL, CODE)).isEqualTo(VerifyResult.LOCKED);
    }

    @Test
    void requestingANewCodeInvalidatesTheOldOne() {
        service.requestCode(EMAIL);
        when(codeGenerator.generate()).thenReturn(OTHER_CODE);
        service.requestCode(EMAIL);

        assertThat(stored).hasSize(2);
        assertThat(stored.get(0).getUsedAt()).isNotNull();
        assertThat(service.verifyCode(EMAIL, CODE)).isEqualTo(VerifyResult.INVALID);
        assertThat(service.verifyCode(EMAIL, OTHER_CODE).isSuccess()).isTrue();
    }

    @Test
    void unknownUserGetsNothingAndNothingIsSent() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        String contact = service.requestCode("nobody@example.com");

        assertThat(contact).isEqualTo("nobody@example.com");
        verifyNoInteractions(notificationService);
        assertThat(stored).isEmpty();
    }

    @Test
    void inactiveUserGetsNothingAndNothingIsSent() {
        user.setActive(false);

        service.requestCode(EMAIL);

        verifyNoInteractions(notificationService);
        assertThat(stored).isEmpty();
    }

    @Test
    void invalidContactIsRejected() {
        assertThatThrownBy(() -> service.requestCode("not a contact @"))
                .isInstanceOf(InvalidContactException.class);
        assertThatThrownBy(() -> service.requestCode("12345"))
                .isInstanceOf(InvalidContactException.class);

        verifyNoInteractions(notificationService);
    }

    @Test
    void verifyWithAnInvalidContactOrUnknownDestinationIsInvalid() {
        assertThat(service.verifyCode("12345", CODE)).isEqualTo(VerifyResult.INVALID);
        assertThat(service.verifyCode(EMAIL, CODE)).isEqualTo(VerifyResult.INVALID);
    }

    @Test
    void theDatabaseNeverStoresThePlainCode() {
        service.requestCode(EMAIL);

        OtpCode otp = stored.get(0);
        assertThat(otp.getCodeHash()).isNotEqualTo(CODE).doesNotContain(CODE);
        assertThat(new BCryptPasswordEncoder().matches(CODE, otp.getCodeHash())).isTrue();
    }

    @Test
    void emailInputUsesTheEmailChannelAndTheMessageContainsTheCode() {
        service.requestCode(EMAIL);

        assertThat(stored.get(0).getChannel()).isEqualTo(Channel.EMAIL);
        assertThat(stored.get(0).getDestination()).isEqualTo(EMAIL);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyContact(eq(EMAIL), eq(OtpService.SUBJECT), body.capture());
        assertThat(body.getValue()).isEqualTo(
                "Your VetClinic verification code is 123456. It is valid for 5 minutes. Do not share it with anyone.");
    }

    @Test
    void phoneInputUsesTheSmsChannelAndIsNormalized() {
        service.requestCode("0532 123 45 67");

        assertThat(stored.get(0).getChannel()).isEqualTo(Channel.SMS);
        assertThat(stored.get(0).getDestination()).isEqualTo(PHONE);
        verify(notificationService).notifyContact(eq(PHONE), any(), any());
    }

    @Test
    void codeExpiresFiveMinutesAfterTheRequest() {
        service.requestCode(EMAIL);

        assertThat(stored.get(0).getExpiresAt().toInstant()).isEqualTo(clock.instant().plus(Duration.ofMinutes(5)));
    }
}
