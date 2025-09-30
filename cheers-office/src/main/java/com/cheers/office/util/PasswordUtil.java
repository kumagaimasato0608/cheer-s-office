package com.cheers.office.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {

    // SecurityConfigで定義したPasswordEncoderをそのまま利用
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 平文のパスワードをハッシュ化する
     */
    public static String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 平文のパスワードがハッシュ値と一致するか検証する
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}