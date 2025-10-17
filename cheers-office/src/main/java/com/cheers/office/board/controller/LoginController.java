package com.cheers.office.board.controller;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;

@Controller
public class LoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model, @ModelAttribute("errorMessage") String errorMessage, @ModelAttribute("message") String message) {
        return "register";
    }


    @PostMapping("/register")
    public String registerUser(@RequestParam("userName") String userName,
                               @RequestParam("mailAddress") String mailAddress,
                               @RequestParam("password") String password,
                               @RequestParam("confirmPassword") String confirmPassword, // 確認用パスワードを取得
                               RedirectAttributes redirectAttributes) {
        
        if (userRepository.findByMailAddress(mailAddress).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "このメールアドレスは既に登録されています。");
            return "redirect:/register";
        }
        
        // ★ パスワード確認チェックを追加
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "パスワードと確認用パスワードが一致しません。");
            return "redirect:/register";
        }
        
        User newUser = new User();
        
        newUser.setUserId(UUID.randomUUID().toString()); 
        newUser.setUserName(userName);
        newUser.setMailAddress(mailAddress);
        
        newUser.setPassword(passwordEncoder.encode(password)); 
        
        newUser.setGroup("未設定");
        newUser.setIcon("/images/default_icon.png");
        
        userRepository.save(newUser);

        // ★★★ 修正: アラート表示用のフラッシュ属性を設定 ★★★
        redirectAttributes.addFlashAttribute("registrationSuccessAlert", "登録されました。"); 
        
        return "redirect:/login";
    }
}