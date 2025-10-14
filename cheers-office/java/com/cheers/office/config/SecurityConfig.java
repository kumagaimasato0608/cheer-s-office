package com.cheers.office.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ------------------ CSRF設定 ------------------
            .csrf(csrf -> csrf
                // CookieにCSRFトークンを保存（JSから取得可能）
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // APIでのCSRF検証を除外（fetch通信を通すため）
                .ignoringRequestMatchers(
                    "/api/**",          // 掲示板API・チャットAPIなど
                    "/mypage/uploadIcon",
                    "/ws/**"            // WebSocket通信
                )
            )

            // ------------------ 認可設定 ------------------
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login", "/register", "/error",
                    "/css/**", "/js/**",
                    "/images/**", "/uploads/**",
                    "/favicon.ico",
                    "/ws/**" // WebSocket許可
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
            );

        // ✅ 同一オリジン（同一ドメイン内）でのiframe許可
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    // ------------------ パスワード暗号化設定 ------------------
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
