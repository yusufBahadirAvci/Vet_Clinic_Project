package com.vetclinic.auth;

import com.vetclinic.AbstractIntegrationTest;
import com.vetclinic.auth.domain.OtpCode;
import com.vetclinic.auth.repository.OtpCodeRepository;
import com.vetclinic.notification.repository.SmsOutboxRepository;
import com.vetclinic.user.domain.User;
import com.vetclinic.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.vetclinic.support.TestData.owner;
import static com.vetclinic.support.TestData.vet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full login flow through the real filter chain and a real database. Users have phones, so codes go to sms_outbox. */
@AutoConfigureMockMvc
class LoginFlowIT extends AbstractIntegrationTest {

    private static final String KNOWN_CODE = "424242";
    private static final String SENT_MESSAGE = "If your details are registered, a verification code has been sent.";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OtpCodeRepository otpCodeRepository;
    @Autowired
    private SmsOutboxRepository smsOutboxRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM otp_codes");
        jdbcTemplate.update("DELETE FROM sms_outbox");
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE '%@loginflow.test' OR phone IN ('+905320000001', '+905320000002')");
    }

    @Test
    void ownerLogsInWithTheCodeAndReachesTheirPets() throws Exception {
        saveUser(owner("Flow Owner", null), "+905320000001");
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/login").session(session).with(csrf()).param("contact", "0532 000 00 01"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login/verify"))
                .andExpect(flash().attribute("successMessage", SENT_MESSAGE));
        assertThat(smsOutboxRepository.findTop50ByOrderByCreatedAtDesc()).hasSize(1);

        replaceStoredCodeWithKnownOne("+905320000001");

        mockMvc.perform(get("/login/verify").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("+90532*****01")));

        MvcResult verified = mockMvc.perform(post("/login/verify").session(session).with(csrf())
                        .param("code", KNOWN_CODE))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/my/pets"))
                .andReturn();

        MockHttpSession loggedIn = (MockHttpSession) verified.getRequest().getSession(false);
        mockMvc.perform(get("/my/pets").session(loggedIn))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You have no registered pets.")));
    }

    @Test
    void vetIsSentToTheDashboard() throws Exception {
        saveUser(vet("Flow Vet", "vet@loginflow.test"), null);
        MockHttpSession session = new MockHttpSession();
        // The e-mail channel would need a mail server, so the vet gets the code through the phone number too.
        jdbcTemplate.update("UPDATE users SET phone = '+905320000002' WHERE email = 'vet@loginflow.test'");

        mockMvc.perform(post("/login").session(session).with(csrf()).param("contact", "05320000002"))
                .andExpect(redirectedUrl("/login/verify"));
        replaceStoredCodeWithKnownOne("+905320000002");

        mockMvc.perform(post("/login/verify").session(session).with(csrf()).param("code", KNOWN_CODE))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/vet"));
    }

    @Test
    void unknownContactGetsTheSameAnswerAndNothingIsSent() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/login").session(session).with(csrf()).param("contact", "0532 000 00 03"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login/verify"))
                .andExpect(flash().attribute("successMessage", SENT_MESSAGE));

        assertThat(smsOutboxRepository.count()).isZero();
        assertThat(otpCodeRepository.count()).isZero();
    }

    @Test
    void invalidContactShowsAFormError() throws Exception {
        mockMvc.perform(post("/login").with(csrf()).param("contact", "hello"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter a valid e-mail address or mobile number")));
    }

    @Test
    void wrongCodeShowsTheGenericMessage() throws Exception {
        saveUser(owner("Flow Owner", null), "+905320000001");
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/login").session(session).with(csrf()).param("contact", "+905320000001"));

        mockMvc.perform(post("/login/verify").session(session).with(csrf()).param("code", "000000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The code is invalid or has expired")));
    }

    @Test
    void verifyPageWithoutAnActiveRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/login/verify"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    private void saveUser(User user, String phone) {
        user.setPhone(phone);
        if (user.getEmail() == null && phone == null) {
            throw new IllegalArgumentException("a user needs a contact");
        }
        userRepository.save(user);
    }

    private void replaceStoredCodeWithKnownOne(String destination) {
        OtpCode otp = otpCodeRepository.findTopByDestinationOrderByCreatedAtDesc(destination).orElseThrow();
        otp.setCodeHash(passwordEncoder.encode(KNOWN_CODE));
        otpCodeRepository.save(otp);
    }
}
