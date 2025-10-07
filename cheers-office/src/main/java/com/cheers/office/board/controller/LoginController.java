package com.cheers.office.board.controller;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder; // ★ これをimport
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;
// import com.cheers.office.util.PasswordUtil; // ★ PasswordUtilはもう不要

@Controller
public class LoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // ★ PasswordEncoderをフィールドに追加

    // ★ コンストラクタを修正して、PasswordEncoderも受け取る
    public LoginController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // (showLoginForm, showRegisterFormメソッドは変更なし)
    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model, @ModelAttribute("errorMessage") String errorMessage, @ModelAttribute("message") String message) {
        // ... (中身は変更なし)
        return "register";
    }


    @PostMapping("/register")
    public String registerUser(@RequestParam("userName") String userName,
                               @RequestParam("mailAddress") String mailAddress,
                               @RequestParam("password") String password,
                               @RequestParam("confirmPassword") String confirmPassword,
                               RedirectAttributes redirectAttributes) {
        
        // (バリデーションチェックは変更なし)
        // ...

        if (userRepository.findByMailAddress(mailAddress).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "このメールアドレスは既に登録されています。");
            return "redirect:/register";
        }
        
        User newUser = new User();
        
        newUser.setUserId(UUID.randomUUID().toString()); 
        newUser.setUserName(userName);
        newUser.setMailAddress(mailAddress);
        
        // ★★★ ここを修正！ ★★★
        // PasswordUtil.encode(password) の代わりに、注入したpasswordEncoderを使う
        newUser.setPassword(passwordEncoder.encode(password)); 
        
        // (その他の初期値設定は変更なし)
        // ...
        newUser.setGroup("未設定");
        newUser.setIcon("/images/default_icon.png");
        
        userRepository.save(newUser);

        redirectAttributes.addFlashAttribute("message", "アカウントが正常に登録されました。ログインしてください。");
        return "redirect:/login";
    }
}