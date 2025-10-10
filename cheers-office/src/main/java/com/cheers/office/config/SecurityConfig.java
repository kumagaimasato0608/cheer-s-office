package com.cheers.office.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
// import org.springframework.security.web.util.matcher.AntPathRequestMatcher; // ← 古いので不要

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ------------------ CSRF設定 ------------------
            .csrf(csrf -> csrf
                // JavaScriptからトークンを取得できるようCookieに保存
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // ★★★ APIでのCSRF無効化を削除（より安全な状態） ★★★
                // .ignoringRequestMatchers("/api/**", "/mypage/uploadIcon") // ← この行を削除またはコメントアウト
            )

            // ------------------ 認可設定 ------------------
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login", "/register", "/error",
                    "/css/**", "/js/**",
                    "/images/**",
                    "/uploads/**",
                    "/favicon.ico",
                    "/ws/**" // ★ WebSocket接続を許可
                ).permitAll()
                .anyRequest().authenticated()
            )

            // ------------------ ログイン設定 ------------------
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/home", true) // ログイン成功後の遷移先を固定
                .failureUrl("/login?error=true")
                .permitAll()
            )

            // ------------------ ログアウト設定 ------------------
            .logout(logout -> logout
                // .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // ← 古い書き方
                .logoutUrl("/logout") // ★ 新しい書き方
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}