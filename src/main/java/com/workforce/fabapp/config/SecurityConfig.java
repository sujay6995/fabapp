package com.workforce.fabapp.config;

import com.workforce.fabapp.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
@EnableCaching
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/health").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/payroll/**").hasRole("ADMIN")
                        .requestMatchers("/api/history/**").hasRole("ADMIN")
                        .requestMatchers("/api/system/**").hasRole("ADMIN")
                        .requestMatchers("/api/approvals/**").hasAnyRole("SUPERVISOR", "ADMIN")
                        .requestMatchers("/api/supervisors/**").hasAnyRole("SUPERVISOR", "ADMIN")
                        .requestMatchers("/api/double-time/**").hasAnyRole("SUPERVISOR", "ADMIN")
                        .requestMatchers("/api/timesheets/**").authenticated()
                        .requestMatchers("/api/leave/**").authenticated()
                        .requestMatchers("/api/schedule/**").authenticated()
                        .requestMatchers("/api/employees/**").authenticated()
                        .requestMatchers("/api/tasks/**").authenticated()
                        .requestMatchers("/api/attendance/**").authenticated()
                        .requestMatchers("/api/job-budgets/**").authenticated()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
