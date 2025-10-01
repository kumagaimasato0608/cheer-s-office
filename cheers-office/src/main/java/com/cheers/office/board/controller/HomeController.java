package com.cheers.office.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * ルートURL ( / ) にアクセスがあった場合、/home にリダイレクトする。
     * Spring Securityが認証後に実行するため、ログイン後の入口となる。
     * * URL: http://localhost:8080/
     */
    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/home"; // /home に転送
    }
    
    /**
     * 実際のホーム画面を表示する。
     * * URL: http://localhost:8080/home
     */
    @GetMapping("/home")
    public String home() {
        return "home"; // src/main/resources/templates/home.html を返す
    }
    
    /**
     * ログイン画面を表示する。
     * AuthControllerとマッピングが重複するため、ここでは削除またはコメントアウト。
     * （今回は AuthController に /login を任せるため、このメソッドは不要）
     * * URL: http://localhost:8080/login
     */
    /*
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    */

    /**
     * マイページを表示する。
     * * URL: http://localhost:8080/mypage
     */
    @GetMapping("/mypage")
    public String mypage() {
        return "mypage"; // src/main/resources/templates/mypage.html を返す
    }

    /**
     * フォトピン画面を表示する。
     * * URL: http://localhost:8080/photopin
     */
    @GetMapping("/photopin")
    public String photopin() {
        return "photopin"; // src/main/resources/templates/photopin.html を返す
    }
}