package com.retailpos.server.admin;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * /admin/api/**  admin session (except setup + login)
 * /sync/v1/**    terminal token, checked by SyncFeedController
 * static admin UI + health public; everything else (e.g. shared modules' POS endpoints) denied.
 */
@Configuration
public class AdminSecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain adminFilterChain(HttpSecurity http, AdminAuth.Sessions sessions) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(new AdminAuth.SessionFilter(sessions), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health", "/", "/index.html", "/assets/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/admin/api/setup/**", "/admin/api/setup", "/admin/api/login").permitAll()
                .requestMatchers("/sync/v1/**").permitAll()
                .requestMatchers("/admin/api/**").hasRole("ADMIN")
                .anyRequest().denyAll());
        return http.build();
    }
}
