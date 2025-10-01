package com.cheers.office.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping; // POST処理のためインポート
import org.springframework.web.bind.annotation.RequestParam; // フォームデータ取得のためインポート
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // リダイレクト時にメッセージを渡すためインポート

import com.cheers.office.board.model.User; // Userモデルのimport
import com.cheers.office.board.repository.UserRepository; // UserRepositoryのimport
import com.cheers.office.util.PasswordUtil; // PasswordUtilのimport

@Controller
public class AuthController {

    private final UserRepository userRepository; // Userデータの保存用

    // コンストラクタインジェクション
    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ログイン画面の表示 (既存のメソッド)
    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    // ★★★ 新規登録画面の表示 ★★★
    @GetMapping("/register")
    public String showRegisterForm() {
        return "register"; // src/main/resources/templates/register.html を表示
    }

    // ★★★ 新規登録の処理 ★★★
    @PostMapping("/register")
    public String registerUser(@RequestParam("userName") String userName,
                               @RequestParam("mailAddress") String mailAddress,
                               @RequestParam("password") String password,
                               RedirectAttributes redirectAttributes) {
        
        // 1. ユーザー名/メールアドレスの重複チェック
        if (userRepository.findByMailAddress(mailAddress).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "このメールアドレスは既に登録されています。");
            return "redirect:/register";
        }
        
        // 2. 新しいUserオブジェクトを作成
        User newUser = new User();
        // IDはRepositoryがUUIDで自動生成するため、ここでは設定しない
        newUser.setUserName(userName);
        newUser.setMailAddress(mailAddress);
        
        // 3. パスワードをハッシュ化して設定
        newUser.setPassword(PasswordUtil.encode(password)); 
        
        // 4. その他の初期値を設定 (掲示板の要求に合わせて、グループなどは必須ではないが設定)
        newUser.setGroup("未設定");
        newUser.setMyBoom("未設定");
        newUser.setHobby("未設定");
        newUser.setIcon("/images/default-avatar.png"); 

        // 5. データベースに保存
        userRepository.save(newUser);

        // 6. 成功メッセージを付けてログイン画面にリダイレクト
        redirectAttributes.addFlashAttribute("message", "アカウントが正常に登録されました。ログインしてください。");
        return "redirect:/login";
    }
}