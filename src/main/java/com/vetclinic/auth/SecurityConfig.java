package com.vetclinic.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import com.vetclinic.user.repository.UserRepository;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
public class SecurityConfig {

    private static final String LOGIN_URL = "/login";

    /** Hashes one-time codes (BCrypt); the plain code is never stored. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment,
                                                   UserRepository userRepository) throws Exception {
        boolean devProfile = environment.acceptsProfiles(Profiles.of("dev"));

        http
                .addFilterAfter(new SessionRevalidationFilter(userRepository, LOGIN_URL), SecurityContextHolderFilter.class)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/", "/privacy", "/adoptions", "/adoptions/*").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/adoptions/*/apply", "/login/resend").permitAll();
                    auth.requestMatchers("/login", "/login/verify").permitAll();
                    auth.requestMatchers("/css/**", "/js/**", "/images/**", "/error").permitAll();
                    if (devProfile) {
                        auth.requestMatchers("/dev/login", "/dev/login/**").permitAll();
                    }
                    auth.requestMatchers("/my/**").hasRole("OWNER");
                    auth.requestMatchers("/vet/**").hasRole("VET");
                    auth.anyRequest().authenticated();
                })
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(LOGIN_URL)))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"));
        // CSRF protection stays enabled (default)
        return http.build();
    }
}
