package com.cheers.office.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.util.matcher.AntPathRequestMatcher; // ★ 削除するインポート

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
                // ログインページと登録ページはアクセスを許可
                .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // その他のリクエストは認証が必要
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login") 
                .defaultSuccessUrl("/home", true) // ログイン成功後のリダイレクト先
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                // ★★★ 修正箇所: AntPathRequestMatcher の代わりにラムダ式を使用 ★★★
                // 修正前: .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutRequestMatcher(request -> "/logout".equals(request.getRequestURI())) 
                .logoutSuccessUrl("/login?logout") 
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .permitAll()
            )
            // WebSocketハンドシェイクパスのCSRF保護を無効化
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/ws/**") 
            );
        
        return http.build();
    }
}