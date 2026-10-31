package com.vetclinic.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class TimeConfigTest {

    @Test
    void clockUsesIstanbulZone() {
        assertThat(new TimeConfig().clock().getZone()).isEqualTo(ZoneId.of("Europe/Istanbul"));
    }
}
