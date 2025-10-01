package com.cheers.office.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute; // FlashAttributeを受け取るために追加
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // FlashAttributeを使うため残す

import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;
import com.cheers.office.util.PasswordUtil;

@Controller
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    // ★★★ 修正箇所: FlashAttributeからのerrorMessageを受け取るようにする ★★★
    @GetMapping("/register")
    public String showRegisterForm(@ModelAttribute("errorMessage") String errorMessage, Model model) {
        // RedirectAttributesからのerrorMessageが自動的にmodelにセットされるようにする
        // または、直接 @RequestParam でエラーメッセージを受け取るようにしても良い
        // 現状、RedirectAttributes.addFlashAttribute("errorMessage", ...) を使っているので、
        // そのメッセージは自動的にModelに渡されます。
        // ここでは、もしFlashAttributeにerrorMessageがあればそれをModelに追加する
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return "register";
    }


    @PostMapping("/register")
    public String registerUser(@RequestParam("userName") String userName,
                               @RequestParam("mailAddress") String mailAddress,
                               @RequestParam("password") String password,
                               @RequestParam("confirmPassword") String confirmPassword,
                               RedirectAttributes redirectAttributes) {
        
        // ★★★ パスワードの一致チェック (このロジックは正しいです) ★★★
        if (!password.equals(confirmPassword)) {
            // エラーメッセージをセットして登録画面にリダイレクト
            redirectAttributes.addFlashAttribute("errorMessage", "パスワードが一致しません");
            return "redirect:/register"; // ここで処理が中断され、リダイレクトされる
        }

        // メールアドレスの重複チェック
        if (userRepository.findByMailAddress(mailAddress).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "このメールアドレスは既に登録されています。");
            return "redirect:/register";
        }
        
        User newUser = new User();
        newUser.setUserName(userName);
        newUser.setMailAddress(mailAddress);
        newUser.setPassword(PasswordUtil.encode(password));
        
        newUser.setGroup("未設定");
        newUser.setMyBoom("未設定");
        newUser.setHobby("未設定");
        newUser.setIcon("/images/default-avatar.png"); 

        userRepository.save(newUser);

        redirectAttributes.addFlashAttribute("message", "アカウントが正常に登録されました。ログインしてください。");
        return "redirect:/login";
    }
}