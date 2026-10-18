/*
 * Green Launcher Website - Public downloads and release management.
 * Copyright (C) 2026  Berke Akçen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.despical.greenweb.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configures the administrator account, CSRF-protected login and public route permissions.
 * Production startup requires a BCrypt password hash; only the local profile may create a password
 * file.
 *
 * @author Despical
 */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_READ_ROUTES = {
        "/",
        "/features",
        "/faq",
        "/changelog",
        "/changelog/**",
        "/download",
        "/downloads/**",
        "/updates/**",
        "/actuator/health",
        "/robots.txt",
        "/sitemap.xml"
    };

    @Bean
    UserDetailsService admin(
            @Value("${app.admin-username}") String username,
            @Value("${app.admin-password-hash:}") String hash,
            @Value("${app.local-admin:false}") boolean local,
            @Value("${app.storage}") String storage)
            throws IOException {
        if (hash.isBlank()) {
            if (!local) {
                throw new IllegalStateException(
                        "Set SITE_ADMIN_PASSWORD_HASH to a BCrypt hash for production.");
            }
            Path file =
                    Path.of(storage)
                            .toAbsolutePath()
                            .normalize()
                            .resolve("local-admin-password.txt");
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                byte[] bytes = new byte[24];
                new SecureRandom().nextBytes(bytes);
                Files.writeString(
                        file, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
            }
            hash = new BCryptPasswordEncoder(12).encode(Files.readString(file).strip());
        }
        if (!hash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")) {
            throw new IllegalStateException("SITE_ADMIN_PASSWORD_HASH must be a BCrypt hash.");
        }
        return new InMemoryUserDetailsManager(
                User.withUsername(username).password("{bcrypt}" + hash).roles("ADMIN").build());
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/admin/login",
                                                "/error",
                                                "/css/**",
                                                "/js/**",
                                                "/images/**",
                                                "/fonts/**")
                                        .permitAll()
                                        .requestMatchers("/admin/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(HttpMethod.GET, PUBLIC_READ_ROUTES)
                                        .permitAll()
                                        .requestMatchers(HttpMethod.HEAD, PUBLIC_READ_ROUTES)
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/language")
                                        .permitAll()
                                        .anyRequest()
                                        .denyAll())
                .formLogin(
                        login ->
                                login.loginPage("/admin/login")
                                        .loginProcessingUrl("/admin/login")
                                        .defaultSuccessUrl("/admin", true)
                                        .failureUrl("/admin/login?error")
                                        .permitAll())
                .logout(
                        logout ->
                                logout.logoutUrl("/admin/logout")
                                        .logoutSuccessUrl("/admin/login?logout"))
                .headers(
                        headers ->
                                headers.contentSecurityPolicy(
                                        csp ->
                                                csp.policyDirectives(
                                                        "default-src 'self'; script-src 'self'"
                                                            + " https://www.googletagmanager.com;"
                                                            + " style-src 'self'; img-src 'self' data:"
                                                            + " https://*.google-analytics.com"
                                                            + " https://*.googletagmanager.com;"
                                                            + " font-src 'self'; connect-src 'self'"
                                                            + " https://*.google-analytics.com"
                                                            + " https://*.analytics.google.com"
                                                            + " https://*.googletagmanager.com;"
                                                            + " frame-ancestors 'none'; base-uri"
                                                            + " 'self'; form-action 'self';"
                                                            + " object-src 'none'")))
                .addFilterBefore(new LoginThrottle(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    static class LoginThrottle extends OncePerRequestFilter {

        record Attempts(long start, int count) {}

        final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            if (request.getMethod().equals("POST")
                    && request.getServletPath().equals("/admin/login")) {
                long now = System.currentTimeMillis();
                attempts.entrySet().removeIf(e -> now - e.getValue().start() > 300000);
                if (attempts.size() > 10000) {
                    response.sendError(429);
                    return;
                }
                Attempts current =
                        attempts.compute(
                                request.getRemoteAddr(),
                                (key, old) ->
                                        old == null
                                                ? new Attempts(now, 1)
                                                : new Attempts(old.start(), old.count() + 1));
                if (current.count() > 10) {
                    response.setHeader("Retry-After", "300");
                    response.sendError(429);
                    return;
                }
            }
            chain.doFilter(request, response);
        }
    }
}
