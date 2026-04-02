package com.year2.queryme.config;

import com.year2.queryme.security.JwtAuthFilter;
import com.year2.queryme.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    // ✅ Constructor Injection (BEST PRACTICE)
    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
            UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // ── Public endpoints ─────────────────────────────
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/teachers/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/admins/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/guests/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/courses").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/class-groups").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/courses").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/class-groups/**").permitAll()

                        // ── Public read ─────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/courses").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/class-groups/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/teachers").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PUT, "/api/teachers/**").hasRole("TEACHER")

                        // ── TEACHER or ADMIN ───────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/students/register")
                        .hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/students")
                        .hasAnyRole("TEACHER", "ADMIN")

                        // ── STUDENT, TEACHER, ADMIN ────────────────────
                        .requestMatchers(HttpMethod.PUT, "/api/students/**")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                        // ── ADMIN ──────────────────────────────────────
                        .requestMatchers("/api/admins/**").hasRole("ADMIN")

                        // ── GUEST ──────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/guests").hasRole("GUEST")
                        .requestMatchers(HttpMethod.PUT, "/api/guests/**").hasRole("GUEST")

                        // ── Authenticated ──────────────────────────────
                        .requestMatchers("/api/auth/me").authenticated()

                        // ── Everything else ────────────────────────────
                        .anyRequest().authenticated())

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(403);
                            response.getWriter().write("{\"error\": \"Access Denied\", \"message\": \"" + authException.getMessage() + "\"}");
                        }))

                // ✅ FIXED Authentication Provider
                .authenticationProvider(authenticationProvider())

                // ✅ JWT Filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ✅ Password Encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ FIXED (Spring Boot 4 compatible)
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ✅ Authentication Manager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
