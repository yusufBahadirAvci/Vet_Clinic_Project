package com.vetclinic.common.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    public static final ZoneId CLINIC_ZONE = ZoneId.of("Europe/Istanbul");

    @Bean
    public Clock clock() {
        return Clock.system(CLINIC_ZONE);
    }
}
