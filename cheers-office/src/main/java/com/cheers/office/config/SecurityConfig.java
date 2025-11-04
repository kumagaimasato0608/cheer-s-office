package com.cheers.office.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // ログイン・登録・静的リソース・WebSocket関連は全て許可
                .requestMatchers(
                    "/login", "/register",
                    "/css/**", "/js/**", "/images/**", "/webjars/**",
                    "/ws/**", "/topic/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(request -> "/logout".equals(request.getRequestURI()))
                .logoutSuccessUrl("/login?logout")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .permitAll()
            )
            // ✅ WebSocketハンドシェイクやSTOMP通信のCSRF保護を除外
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/ws/**", "/topic/**", "/app/**")
            )
            // ✅ ★ これが「WebSocketでログイン情報を維持」する最重要設定！
            .securityContext(context -> context.requireExplicitSave(false));

        return http.build();
    }
}
