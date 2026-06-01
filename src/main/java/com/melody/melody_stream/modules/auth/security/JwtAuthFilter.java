package com.melody.melody_stream.modules.auth.security;

import com.melody.melody_stream.modules.auth.dto.response.JwtPayload;
import com.melody.melody_stream.modules.auth.service.JwtTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            JwtPayload payload = jwtTokenService.parseAccessToken(token);

            // Build authorities from both roles and permissions
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            if (payload.roles() != null) {
                payload.roles().forEach(r ->
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
            }
            if (payload.permissions() != null) {
                payload.permissions().forEach(p ->
                        authorities.add(new SimpleGrantedAuthority(p)));
            }

            var authentication = new UsernamePasswordAuthenticationToken(payload, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}
