package com.cheers.office.board.controller;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;
import com.cheers.office.util.PasswordUtil; 


@Controller
public class LoginController {

    private final UserRepository userRepository;

    public LoginController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model, @ModelAttribute("errorMessage") String errorMessage, @ModelAttribute("message") String message) {
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
        }
        return "register";
    }

    /**
     * 新規ユーザー登録処理
     */
    @PostMapping("/register")
    public String registerUser(@RequestParam("userName") String userName,
                               @RequestParam("mailAddress") String mailAddress,
                               @RequestParam("password") String password,
                               @RequestParam("confirmPassword") String confirmPassword,
                               RedirectAttributes redirectAttributes) {
        
        // 1. バリデーションチェック
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "パスワードが一致しません");
            return "redirect:/register";
        }
        if (!mailAddress.contains("@")) {
            redirectAttributes.addFlashAttribute("errorMessage", "正しいメールアドレスを入力してください（@が必要です）。");
            return "redirect:/register";
        }
        
        // ★★★ 2. メールアドレスの重複チェック (★findByMailAddressがnullユーザーをフィルタリングする前提) ★★★
        if (userRepository.findByMailAddress(mailAddress).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "このメールアドレスは既に登録されています。");
            return "redirect:/register";
        }
        
        // ★★★ 3. Userオブジェクトの作成とデータセット（不足していたロジックを補完） ★★★
        User newUser = new User();
        
        // 必須データのセット
        newUser.setUserId(UUID.randomUUID().toString()); 
        newUser.setUserName(userName);
        newUser.setMailAddress(mailAddress);
        
        // パスワードをハッシュ化して設定（セキュリティ上の必須要件）
        newUser.setPassword(PasswordUtil.encode(password)); 
        
        // その他の初期値を設定 (NULLにならないように)
        newUser.setGroup("未設定");
        newUser.setMyBoom("未設定");
        newUser.setHobby("未設定");
        newUser.setIcon("/images/default_icon.png");
        newUser.setStatusMessage("よろしくお願いします！");
        
        // 4. リポジトリへの保存
        userRepository.save(newUser);

        redirectAttributes.addFlashAttribute("message", "アカウントが正常に登録されました。ログインしてください。");
        return "redirect:/login";
    }

    // ... (他のメソッド省略) ...
}