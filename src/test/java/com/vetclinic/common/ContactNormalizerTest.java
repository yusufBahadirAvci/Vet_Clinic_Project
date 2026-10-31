package com.vetclinic.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ContactNormalizerTest {

    private final ContactNormalizer normalizer = new ContactNormalizer();

    @ParameterizedTest
    @ValueSource(strings = {
            "05321234567", "5321234567", "905321234567", "+905321234567",
            "0532 123 45 67", "0532-123-45-67", "(0532) 123 45 67", " +90 532 123 45 67 ", "+90(532)123-45-67"
    })
    void normalizesPhoneFormats(String raw) {
        assertThat(normalizer.normalizePhone(raw)).isEqualTo("+905321234567");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "02121234567", "532123456", "53212345678", "4321234567", "+15321234567",
            "+9005321234567", "abc", "0532 123 45 6a", "905321234", "+90"
    })
    void rejectsInvalidPhones(String raw) {
        assertThatThrownBy(() -> normalizer.normalizePhone(raw)).isInstanceOf(InvalidContactException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsBlankPhones(String raw) {
        assertThatThrownBy(() -> normalizer.normalizePhone(raw)).isInstanceOf(InvalidContactException.class);
    }

    @Test
    void lowercasesAndTrimsEmail() {
        assertThat(normalizer.normalizeEmail("  Ceylin.Kucuk@Gmail.COM ")).isEqualTo("ceylin.kucuk@gmail.com");
    }

    @Test
    void lowercasingIsLocaleIndependent() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            assertThat(normalizer.normalizeEmail("INFO@CLINIC.COM")).isEqualTo("info@clinic.com");
        } finally {
            Locale.setDefault(previous);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"plainaddress", "@gmail.com", "a@", "a@b", "a b@c.com", "a@@b.com", "a@b c.com"})
    void rejectsInvalidEmails(String raw) {
        assertThatThrownBy(() -> normalizer.normalizeEmail(raw)).isInstanceOf(InvalidContactException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rejectsBlankEmails(String raw) {
        assertThatThrownBy(() -> normalizer.normalizeEmail(raw)).isInstanceOf(InvalidContactException.class);
    }

    @Test
    void rejectsTooLongEmail() {
        String longEmail = "a".repeat(250) + "@b.co";
        assertThatThrownBy(() -> normalizer.normalizeEmail(longEmail)).isInstanceOf(InvalidContactException.class);
    }

    @Test
    void detectsType() {
        assertThat(normalizer.detectType("a@b.com")).isEqualTo(ContactType.EMAIL);
        assertThat(normalizer.detectType("0532 123 45 67")).isEqualTo(ContactType.PHONE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void detectTypeRejectsBlank(String raw) {
        assertThatThrownBy(() -> normalizer.detectType(raw)).isInstanceOf(InvalidContactException.class);
    }

    @Test
    void normalizeDispatchesByType() {
        assertThat(normalizer.normalize(" A@B.com ")).isEqualTo("a@b.com");
        assertThat(normalizer.normalize("0532 123 45 67")).isEqualTo("+905321234567");
        assertThatThrownBy(() -> normalizer.normalize("12345")).isInstanceOf(InvalidContactException.class);
    }
}
