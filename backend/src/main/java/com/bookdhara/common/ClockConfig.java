package com.bookdhara.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** One shared clock, so tests can control time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
