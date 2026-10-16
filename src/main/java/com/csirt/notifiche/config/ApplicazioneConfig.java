package com.csirt.notifiche.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicazioneConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
