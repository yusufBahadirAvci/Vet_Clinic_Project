package com.vetclinic.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogMaskerTest {

    @Test
    void masksEmail() {
        assertThat(LogMasker.maskEmail("ceylin@gmail.com")).isEqualTo("c*****@gmail.com");
        assertThat(LogMasker.maskEmail("a@b.com")).isEqualTo("a@b.com");
        assertThat(LogMasker.maskEmail("ab@b.com")).isEqualTo("a*@b.com");
    }

    @Test
    void masksPhone() {
        assertThat(LogMasker.maskPhone("+905321234567")).isEqualTo("+90532*****67");
    }

    @Test
    void unknownOrMalformedInputIsFullyHidden() {
        assertThat(LogMasker.maskEmail(null)).isEqualTo("***");
        assertThat(LogMasker.maskEmail("")).isEqualTo("***");
        assertThat(LogMasker.maskEmail("no-at-sign")).isEqualTo("***");
        assertThat(LogMasker.maskEmail("@gmail.com")).isEqualTo("***");
        assertThat(LogMasker.maskEmail("user@")).isEqualTo("***");
        assertThat(LogMasker.maskPhone(null)).isEqualTo("***");
        assertThat(LogMasker.maskPhone("12345")).isEqualTo("***");
    }
}
