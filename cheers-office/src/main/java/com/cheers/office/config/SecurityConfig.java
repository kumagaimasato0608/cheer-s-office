package com.cheers.office.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ------------------ CSRF設定 ------------------
            .csrf(csrf -> csrf
                // Vue.jsなどからトークン取得できるようCookieに保存
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // ✅ ファイルアップロード用APIをCSRF除外（重要！）
                .ignoringRequestMatchers("/api/**", "/mypage/uploadIcon")
            )

            // ------------------ 認可設定 ------------------
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login", "/register", "/error",
                    "/css/**", "/js/**",
                    "/images/**",      // 画像（static/images/）
                    "/uploads/**",     // ✅ プロフィール画像（static/uploads/）
                    "/favicon.ico"
                ).permitAll() // 認証不要
                .anyRequest().authenticated() // それ以外はログイン必須
            )

            // ------------------ ログイン設定 ------------------
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/home", false)
                .failureUrl("/login?error")
                .permitAll()
            )

            // ------------------ ログアウト設定 ------------------
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout")
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
