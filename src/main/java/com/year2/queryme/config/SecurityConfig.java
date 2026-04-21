package com.year2.queryme.config;

import com.year2.queryme.security.JwtAuthFilter;
import com.year2.queryme.security.UserDetailsServiceImpl;
import com.year2.queryme.security.AuthEntryPointJwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt authEntryPointJwt;
    private final List<String> allowedOrigins;

    // ✅ Constructor Injection (BEST PRACTICE)
    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
            UserDetailsServiceImpl userDetailsService,
            AuthEntryPointJwt authEntryPointJwt,
            @Value("${queryme.security.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") String allowedOrigins) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.authEntryPointJwt = authEntryPointJwt;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .headers(headers -> headers
                    .contentTypeOptions(Customizer.withDefaults())
                    .frameOptions(frame -> frame.deny())
                    .xssProtection(xss -> xss.disable())
                    .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                    .cacheControl(Customizer.withDefaults()))

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── Public endpoints ─────────────────────────────
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/signin").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/bootstrap/super-admin").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/bootstrap/reset").permitAll()
                        .requestMatchers(HttpMethod.GET, "/courses").permitAll()
                        .requestMatchers(HttpMethod.GET, "/class-groups/**").permitAll()

                        // ── Restricted write access ──────────────────────
                        .requestMatchers(HttpMethod.POST, "/courses").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.POST, "/class-groups").hasRole("TEACHER")

                        // ── Public read ─────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/courses").permitAll()
                        .requestMatchers(HttpMethod.GET, "/class-groups/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/teachers").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/teachers/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/teachers/register").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admins/register").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/guests/register").hasRole("ADMIN")

                        // ── TEACHER or ADMIN ───────────────────────────
                        .requestMatchers(HttpMethod.POST, "/students/register")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/students/register/bulk")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/students")
                        .hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/course-enrollments/**")
                        .hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/exams/**")
                        .hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/exams/**")
                        .hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/exams/**")
                        .hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/exams/**")
                        .hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/exams/*/questions")
                        .hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/exams/*/questions/**")
                        .hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/results/exam/**")
                        .hasAnyRole("TEACHER", "ADMIN")

                        // ── STUDENT, TEACHER, ADMIN ────────────────────
                        .requestMatchers(HttpMethod.PUT, "/students/**")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/students/*")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/exams/**")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/results/session/**")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/sessions/start")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/sessions/*/submit")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/sessions/*")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/sessions/student/**")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/sessions/exam/**")
                        .hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/query/**")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers("/sandboxes/**")
                        .hasAnyRole("TEACHER", "ADMIN")

                        // ── ADMIN ──────────────────────────────────────
                        .requestMatchers("/admins/**").hasRole("ADMIN")

                        // ── GUEST ──────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/guests").hasRole("GUEST")
                        .requestMatchers(HttpMethod.PUT, "/guests/**").hasRole("GUEST")

                        // ── Everything else ────────────────────────────
                        .anyRequest().authenticated())

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authEntryPointJwt)
                        .accessDeniedHandler(accessDeniedHandler()))

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

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json");
            response.setStatus(403);
            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"" + accessDeniedException.getMessage() + "\"}");
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
