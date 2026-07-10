package com.melody.melody_stream.config;

import com.melody.melody_stream.modules.auth.dto.response.JwtPayload;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditorConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder
                    .getContext()
                    .getAuthentication();

            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return Optional.of("SYSTEM");
            }

            Object principal = auth.getPrincipal();

            if (principal instanceof JwtPayload jwtPayload) {
                return Optional.of(jwtPayload.sub());
            }

            return Optional.of(auth.getName());
        };
    }
}
