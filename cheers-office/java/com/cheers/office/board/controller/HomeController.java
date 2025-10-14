package com.cheers.office.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // ルートURL ( / ) にアクセスがあった場合、/home にリダイレクト
    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/home";
    }
    
    // 実際のホーム画面
    @GetMapping("/home")
    public String home() {
        return "home"; // home.html を返す
    }
    
    // 他のマイページ、フォトピンのメソッドはここから削除されます。
}