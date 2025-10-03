package com.cheers.office.board.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;

@Controller
@RequestMapping("/mypage/password")
public class PasswordChangeController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordChangeController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String showPasswordChangeForm(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
            model.addAttribute("user", customUserDetails.getUser());
        } else {
            return "redirect:/login"; 
        }

        return "password_change";
    }
    
    @PostMapping("/update/email")
    public String updateEmail(
            @RequestParam("currentMailAddress") String currentMailAddress,
            @RequestParam("newMailAddress") String newMailAddress,
            @RequestParam("currentPasswordForEmail") String currentPasswordForEmail,
            RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            redirectAttributes.addFlashAttribute("errorMessage", "セッションが無効です。再度ログインしてください。");
            return "redirect:/login";
        }
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal(); // ここで取得
        User loggedInUser = customUserDetails.getUser();

        if (!passwordEncoder.matches(currentPasswordForEmail, loggedInUser.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "現在のパスワードが間違っています。");
            return "redirect:/mypage/password";
        }
        
        if (newMailAddress.isEmpty() || newMailAddress.equals(currentMailAddress)) {
            redirectAttributes.addFlashAttribute("errorMessage", "新しいメールアドレスが入力されていないか、現在のものと同じです。");
            return "redirect:/mypage/password";
        }
        if (!newMailAddress.contains("@")) {
            redirectAttributes.addFlashAttribute("errorMessage", "正しいメールアドレスを入力してください（@が必要です）。");
            return "redirect:/mypage/password";
        }
        if (userRepository.findByMailAddress(newMailAddress).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "そのメールアドレスは既に登録されています。");
            return "redirect:/mypage/password";
        }
        
        loggedInUser.setMailAddress(newMailAddress);
        User updatedUser = userRepository.update(loggedInUser);
        
        // CustomUserDetails内のUserオブジェクトも更新
        customUserDetails.setUser(updatedUser);
        
        // Spring Securityの認証情報を再構築（メールアドレスが変わったことをセッションに反映）
        Authentication newAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                customUserDetails, // Principalは更新されたCustomUserDetails
                customUserDetails.getPassword(), // パスワードはCustomUserDetailsから取得
                customUserDetails.getAuthorities()); // 権限もCustomUserDetailsから取得
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        redirectAttributes.addFlashAttribute("successMessage", "メールアドレスが更新されました。新しいメールアドレスで再度ログインしてください。");
        return "redirect:/logout"; 
    }


    /**
     * パスワードの更新処理
     * POST /mypage/password/update/password
     */
    @PostMapping("/update/password")
    public String updatePassword(
            @RequestParam("currentMailAddress") String currentMailAddress,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            redirectAttributes.addFlashAttribute("errorMessage", "セッションが無効です。再度ログインしてください。");
            return "redirect:/login";
        }
        // ここで customUserDetails を取得する！
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal(); 
        User loggedInUser = customUserDetails.getUser(); // ログイン中のユーザー情報を取得

        // 1. ログイン中のユーザーIDとフォームから渡されたメールアドレスの整合性チェック（念のため）
        if (!loggedInUser.getMailAddress().equals(currentMailAddress)) {
             redirectAttributes.addFlashAttribute("errorMessage", "現在ログイン中のユーザー情報とフォームの内容が一致しません。");
             return "redirect:/mypage/password";
        }

        // 2. 現在のパスワード認証 (必須)
        if (!passwordEncoder.matches(currentPassword, loggedInUser.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "現在のパスワードが間違っています。");
            return "redirect:/mypage/password";
        }

        // 3. 新しいパスワードのバリデーション
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "新しいパスワードを入力してください。");
            return "redirect:/mypage/password";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "新しいパスワードが確認用と一致しません。");
            return "redirect:/mypage/password";
        }
        if (passwordEncoder.matches(newPassword, loggedInUser.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "新しいパスワードは現在のパスワードと異なるものにしてください。");
            return "redirect:/mypage/password";
        }

        // 4. 更新とセッション処理
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        loggedInUser.setPassword(encodedNewPassword); // ユーザーオブジェクトのパスワードを更新
        User resultUser = userRepository.update(loggedInUser); // リポジトリで永続化

        if (resultUser != null) {
            // SecurityContext内のCustomUserDetailsのUserオブジェクトも更新
            customUserDetails.setUser(resultUser);
            
            // Spring Securityの認証情報を再構築 (パスワードが変わったことをセッションに反映)
            Authentication newAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    customUserDetails, // Principalは更新されたCustomUserDetails
                    encodedNewPassword, // 新しいエンコード済みパスワード
                    customUserDetails.getAuthorities()); // 権限はCustomUserDetailsから取得
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            redirectAttributes.addFlashAttribute("successMessage", "パスワードが正常に更新されました。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "パスワードの更新に失敗しました。");
        }

        return "redirect:/mypage/password";
    }
}