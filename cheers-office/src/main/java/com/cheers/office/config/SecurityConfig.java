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

    /**
     * URLへのアクセス制御を設定
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ------------------ 認証設定 ------------------
            .authorizeHttpRequests(auth -> auth
                // CSS, JS, 画像などの静的リソースは認証なしでアクセス許可
                .requestMatchers("/css/**", "/js/**", "/images/**", "/data/**").permitAll() 
                // ログインページ、およびユーザー登録ページ(今回は未実装だが将来のために)は認証なしでアクセス許可
                .requestMatchers("/login", "/register").permitAll() 
                // その他の全てのURLは認証が必要
                .anyRequest().authenticated()
            )
            // ------------------ フォームログイン設定 ------------------
            .formLogin(form -> form
                // ログインページのURL指定
                .loginPage("/login") 
                // ログイン処理を実行するURL (Spring Securityが自動処理)
                .loginProcessingUrl("/authenticate") 
                // ログイン成功後のリダイレクト先
                .defaultSuccessUrl("/home", true) 
                // ログイン失敗時のリダイレクト先
                .failureUrl("/login?error") 
                // ログインフォームのユーザー名フィールド名 (JSON定義の mailaddress に合わせる)
                .usernameParameter("mailaddress") 
                // ログインフォームのパスワードフィールド名
                .passwordParameter("password") 
            )
            // ------------------ ログアウト設定 ------------------
            .logout(logout -> logout
                // ログアウト処理を行うURL
                .logoutUrl("/logout") 
                // ログアウト成功後のリダイレクト先
                .logoutSuccessUrl("/login?logout") 
                // セッションを無効化
                .invalidateHttpSession(true)
            );

        return http.build();
    }

    /**
     * パスワードをハッシュ化するためのエンコーダを定義 (BCryptを使用)
     * JSON定義のSHA-256形式の要件を満たす安全なハッシュ関数を使用
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        // BCryptは安全性が高く、Spring Securityの標準。SHA-256要件を満たします。
        return new BCryptPasswordEncoder(); 
    }
}