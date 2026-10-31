package com.vetclinic.auth;

import com.vetclinic.auth.domain.OtpCode;
import com.vetclinic.auth.repository.OtpCodeRepository;
import com.vetclinic.common.ContactNormalizer;
import com.vetclinic.common.ContactType;
import com.vetclinic.common.InvalidContactException;
import com.vetclinic.common.LogMasker;
import com.vetclinic.notification.NotificationService;
import com.vetclinic.notification.domain.Channel;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/** Issues and verifies one-time login codes. Neither the code nor a full contact is ever logged. */
@Service
public class OtpService {

    static final Duration VALIDITY = Duration.ofMinutes(5);
    static final int MAX_ATTEMPTS = 5;
    static final String SUBJECT = "Your VetClinic verification code";

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final ContactNormalizer contactNormalizer;
    private final OtpCodeGenerator codeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final Clock clock;

    public OtpService(OtpCodeRepository otpCodeRepository, UserRepository userRepository,
                      ContactNormalizer contactNormalizer, OtpCodeGenerator codeGenerator,
                      PasswordEncoder passwordEncoder, NotificationService notificationService, Clock clock) {
        this.otpCodeRepository = otpCodeRepository;
        this.userRepository = userRepository;
        this.contactNormalizer = contactNormalizer;
        this.codeGenerator = codeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * Sends a new code to the given e-mail or phone. Does nothing when no active user has that contact,
     * so the caller cannot tell whether the contact is registered.
     *
     * @return the normalized contact (also returned for unknown users)
     * @throws InvalidContactException if the input is not a valid e-mail address or mobile number
     */
    @Transactional
    public String requestCode(String rawContact) {
        String contact = contactNormalizer.normalize(rawContact);
        ContactType type = contactNormalizer.detectType(contact);

        Optional<User> found = findUser(contact, type).filter(User::isActive);
        if (found.isEmpty()) {
            log.info("OTP requested for an unknown or inactive contact {}", mask(contact, type));
            return contact;
        }
        User user = found.get();
        OffsetDateTime now = OffsetDateTime.now(clock);

        for (OtpCode old : otpCodeRepository.findAllByUserIdAndUsedAtIsNull(user.getId())) {
            old.setUsedAt(now);
        }

        String code = codeGenerator.generate();
        OtpCode otp = new OtpCode();
        otp.setUser(user);
        otp.setChannel(type == ContactType.EMAIL ? Channel.EMAIL : Channel.SMS);
        otp.setDestination(contact);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setExpiresAt(now.plus(VALIDITY));
        otpCodeRepository.save(otp);

        notificationService.notifyContact(contact, SUBJECT,
                "Your VetClinic verification code is " + code
                        + ". It is valid for 5 minutes. Do not share it with anyone.");
        log.info("OTP sent to {}", mask(contact, type));
        return contact;
    }

    /** Never throws for a wrong code: the increased attempt counter must be committed. */
    @Transactional
    public VerifyResult verifyCode(String rawContact, String code) {
        String contact;
        ContactType type;
        try {
            contact = contactNormalizer.normalize(rawContact);
            type = contactNormalizer.detectType(contact);
        } catch (InvalidContactException e) {
            return VerifyResult.INVALID;
        }

        Optional<User> user = findUser(contact, type).filter(User::isActive);
        Optional<OtpCode> latest = otpCodeRepository.findTopByDestinationOrderByCreatedAtDesc(contact);
        if (user.isEmpty() || latest.isEmpty()) {
            return VerifyResult.INVALID;
        }
        OtpCode otp = latest.get();
        if (otp.getAttemptCount() >= MAX_ATTEMPTS) {
            return VerifyResult.LOCKED;
        }
        if (otp.getUsedAt() != null || !OffsetDateTime.now(clock).isBefore(otp.getExpiresAt())) {
            return VerifyResult.INVALID;
        }

        if (code == null || !passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpCodeRepository.save(otp);
            if (otp.getAttemptCount() >= MAX_ATTEMPTS) {
                log.warn("OTP locked after {} wrong attempts for {}", MAX_ATTEMPTS, mask(contact, type));
                return VerifyResult.LOCKED;
            }
            return VerifyResult.INVALID;
        }

        otp.setUsedAt(OffsetDateTime.now(clock));
        otpCodeRepository.save(otp);
        return VerifyResult.success(user.get());
    }

    private Optional<User> findUser(String contact, ContactType type) {
        return type == ContactType.EMAIL ? userRepository.findByEmail(contact) : userRepository.findByPhone(contact);
    }

    private static String mask(String contact, ContactType type) {
        return type == ContactType.EMAIL ? LogMasker.maskEmail(contact) : LogMasker.maskPhone(contact);
    }
}
