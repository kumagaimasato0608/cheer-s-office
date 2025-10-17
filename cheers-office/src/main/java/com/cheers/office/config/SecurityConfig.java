package com.cheers.office.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ------------------ CSRF設定 ------------------
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                    "/api/**",          // REST API
                    "/mypage/uploadIcon",
                    "/api/chat/upload",
                    "/ws/**"            // WebSocket
                )
            )

            // ------------------ 認可設定 ------------------
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login", "/register", "/error",
                    "/css/**", "/js/**",
                    "/images/**", "/uploads/**",
                    "/favicon.ico",
                    "/ws/**"
                ).permitAll()
                .anyRequest().authenticated()
            )

            // ------------------ ログイン設定 ------------------
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )

            // ------------------ ログアウト設定 ------------------
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // ------------------ 同一オリジンiframe許可 ------------------
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    // ------------------ パスワード暗号化 ------------------
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
