package com.cheers.office.board.controller; 

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cheers.office.board.dto.EmailUpdateForm;
import com.cheers.office.board.dto.PasswordUpdateForm;
import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;
import com.cheers.office.board.service.UserAccountService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/mypage")
public class MypageController {

    private final UserRepository userRepository;
    private final UserAccountService userAccountService;
    private final ObjectMapper objectMapper = new ObjectMapper(); 

    public MypageController(UserRepository userRepository, UserAccountService userAccountService) {
        this.userRepository = userRepository;
        this.userAccountService = userAccountService;
    }
    
    // --- 既存のメソッド（プロフィール表示・更新） ---
    
    /**
     * プロフィール編集画面の表示 (GET /mypage)
     */
    @GetMapping
    public String mypage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
            model.addAttribute("user", customUserDetails.getUser());
        } else {
            return "redirect:/login"; 
        }
        return "mypage";
    }
    
    /**
     * プロフィール情報の更新処理 (POST /mypage/update)
     */
    @PostMapping("/update")
    public String updateProfile(User updatedUser, RedirectAttributes redirectAttributes) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            redirectAttributes.addFlashAttribute("errorMessage", "セッションが無効です。再度ログインしてください。");
            return "redirect:/login";
        }
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = customUserDetails.getUser();
        
        // 既存の値を保持
        updatedUser.setMailAddress(currentUser.getMailAddress());
        updatedUser.setPassword(currentUser.getPassword()); 
        updatedUser.setUserId(currentUser.getUserId());
        
        // UserRepositoryで更新を実行
        User resultUser = userRepository.update(updatedUser); 
        
        if (resultUser != null) { 
            customUserDetails.setUser(resultUser);
            redirectAttributes.addFlashAttribute("successMessage", "プロフィール情報が更新されました。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "プロフィールの更新に失敗しました。");
        }
        
        return "redirect:/mypage"; 
    }

    // --- パスワード・メールアドレス変更メソッド ---

    /**
     * パスワード/メールアドレス変更画面の表示 (GET /mypage/password)
     */
    @GetMapping("/password")
    public String showPasswordChangeForm(
        Model model, 
        @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        model.addAttribute("user", customUserDetails.getUser());
        return "password_change";
    }

    /**
     * メールアドレスの更新処理 (POST /mypage/password/update/email)
     */
    @PostMapping("/password/update/email")
    public String updateEmail(
            @ModelAttribute EmailUpdateForm form, 
            RedirectAttributes redirectAttributes) {

        boolean success = userAccountService.updateEmail(
            form.getCurrentMailAddress(),
            form.getCurrentPasswordForEmail(),
            form.getNewMailAddress()
        );
        
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "メールアドレスが更新されました。新しいメールアドレスで再度ログインしてください。");
            return "redirect:/logout";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "現在のパスワードが正しくないか、そのメールアドレスは既に使用されています。");
            return "redirect:/mypage/password";
        }
    }


    /**
     * パスワードの更新処理 (POST /mypage/password/update/password)
     */
    @PostMapping("/password/update/password")
    public String updatePassword(
            @ModelAttribute PasswordUpdateForm form, 
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "新しいパスワードが確認用と一致しません。");
            return "redirect:/mypage/password";
        }
        
        Optional<User> updatedUserOpt = userAccountService.updatePassword(
            form.getCurrentMailAddress(),
            form.getCurrentPassword(),
            form.getNewPassword()
        );

        if (updatedUserOpt.isPresent()) {
            customUserDetails.setUser(updatedUserOpt.get());
            redirectAttributes.addFlashAttribute("successMessage", "パスワードが正常に更新されました。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "現在のパスワードが正しくないか、新しいパスワードが現在のものと同じです。");
        }
        
        return "redirect:/mypage/password";
    }

    // --- アイコンアップロードメソッド ---

    /**
     * プロフィールアイコンのアップロードと保存処理 (POST /mypage/uploadIcon)
     */
    @PostMapping("/uploadIcon")
    @ResponseBody 
    public String uploadIcon(
        @RequestParam("file") MultipartFile file, 
        @AuthenticationPrincipal CustomUserDetails userDetails) throws JsonProcessingException {

        String userId = userDetails.getUser().getUserId();

        if (file.isEmpty()) {
            return objectMapper.writeValueAsString(new ResponseDto(false, "ファイルが空です。"));
        }

        try {
            String newIconPath = userAccountService.saveProfileIcon(file, userId);
            
            User updatedUser = userAccountService.updateUserIcon(userId, newIconPath);
            userDetails.setUser(updatedUser); 

            return objectMapper.writeValueAsString(new IconResponseDto(true, "アイコンが更新されました。", newIconPath));

        } catch (Exception e) {
            System.err.println("Icon Upload Error for user " + userId + ": " + e.getMessage());
            return objectMapper.writeValueAsString(new ResponseDto(false, "アイコンの保存中にサーバーエラーが発生しました。"));
        }
    }
    
    // --- 警告を解消するための修正済み JSONレスポンス用内部クラス ---

    @SuppressWarnings("unused") 
    private static class ResponseDto {
        private boolean success;
        private String message;
        
        public ResponseDto(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    @SuppressWarnings("unused")
    private static class IconResponseDto extends ResponseDto {
        private String iconPath;
        
        public IconResponseDto(boolean success, String message, String iconPath) {
            super(success, message);
            this.iconPath = iconPath;
        }
        
        public String getIconPath() { return iconPath; }
    }
}