package com.cheers.office.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// 認証に関わる画面遷移を処理するコントローラー
@Controller
public class AuthController {

    // ログインページの表示 (URL: /login)
    @GetMapping("/login")
    public String showLoginForm() {
        return "login"; // src/main/resources/templates/login.html を参照
    }

    // ログイン成功後のホーム画面 (URL: /home)
    @GetMapping("/home")
    public String showHome() {
        // 今後のステップでユーザー情報や通知データをThymeleafに渡すロジックを追加
        return "home"; // src/main/resources/templates/home.html を参照
    }
}