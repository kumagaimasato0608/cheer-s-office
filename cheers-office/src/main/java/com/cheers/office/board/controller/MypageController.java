package com.cheers.office.board.controller; 

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
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

import lombok.Data; // ★★★ Lombokのimportを追加 ★★★

@Controller
public class MypageController {

    private final UserRepository userRepository;
    private final UserAccountService userAccountService;

    // パスワードの最小文字数定義
    private static final int MIN_PASSWORD_LENGTH = 8; 

    @Value("${app.upload-dir.profile:src/main/resources/static/images/profile}")
    private String profileUploadDir;
    
    public MypageController(UserRepository userRepository, UserAccountService userAccountService) {
        this.userRepository = userRepository;
        this.userAccountService = userAccountService;
    }
    
    @GetMapping("/mypage")
    public String mypage(Model model, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (customUserDetails != null) {
            model.addAttribute("user", customUserDetails.getUser());
        } else {
            return "redirect:/login"; 
        }
        return "mypage";
    }
    
    @PostMapping("/mypage/update")
    public String updateProfile(@ModelAttribute("user") User formUser, 
                                @AuthenticationPrincipal CustomUserDetails customUserDetails, 
                                RedirectAttributes redirectAttributes) {
        
        if (customUserDetails == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "セッションが無効です。再度ログインしてください。");
            return "redirect:/login";
        }
        
        Optional<User> currentUserOpt = userRepository.findById(customUserDetails.getUser().getUserId());
        if (currentUserOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "ユーザー情報が見つかりません。");
            return "redirect:/login";
        }
        User currentUser = currentUserOpt.get();
        
        // 既存フィールドの更新
        currentUser.setUserName(formUser.getUserName());
        currentUser.setStatusMessage(formUser.getStatusMessage());
        currentUser.setGroup(formUser.getGroup());
        currentUser.setHobby(formUser.getHobby());
        currentUser.setMyBoom(formUser.getMyBoom());
        
        // 配属先情報フィールドの更新
        currentUser.setDeploymentDestination(formUser.getDeploymentDestination());
        currentUser.setDeploymentArea(formUser.getDeploymentArea());
        currentUser.setCommuteFrequency(formUser.getCommuteFrequency());
        currentUser.setWorkTime(formUser.getWorkTime());
        currentUser.setWorkContent(formUser.getWorkContent());
        
        userRepository.save(currentUser); 
        
        customUserDetails.setUser(currentUser);
        
        redirectAttributes.addFlashAttribute("successMessage", "プロフィール情報が更新されました。");
        return "redirect:/mypage"; 
    }

    @GetMapping("/mypage/password")
    public String showPasswordChangeForm(Model model, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        model.addAttribute("user", customUserDetails.getUser());
        model.addAttribute("passwordUpdateForm", new PasswordUpdateForm());
        model.addAttribute("emailUpdateForm", new EmailUpdateForm());
        return "password_change";
    }

    @PostMapping("/mypage/password/update/email")
    public String updateEmail(@ModelAttribute EmailUpdateForm form, 
                              @AuthenticationPrincipal CustomUserDetails customUserDetails,
                              RedirectAttributes redirectAttributes) {
        boolean success = userAccountService.updateEmail(customUserDetails.getUsername(), form.getCurrentPasswordForEmail(), form.getNewMailAddress());
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "メールアドレスが更新されました。新しいメールアドレスで再度ログインしてください。");
            return "redirect:/logout";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "現在のパスワードが正しくないか、そのメールアドレスは既に使用されています。");
            return "redirect:/mypage/password";
        }
    }

    
    @PostMapping("/mypage/password/update/password")
    public String updatePassword(@ModelAttribute PasswordUpdateForm form, 
                                 RedirectAttributes redirectAttributes,
                                 @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        if (form.getNewPassword().length() < MIN_PASSWORD_LENGTH) {
            redirectAttributes.addFlashAttribute("errorMessage", "新しいパスワードは" + MIN_PASSWORD_LENGTH + "文字以上で入力してください。");
            return "redirect:/mypage/password";
        }
        
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "新しいパスワードが確認用と一致しません。");
            return "redirect:/mypage/password";
        }
        Optional<User> updatedUserOpt = userAccountService.updatePassword(customUserDetails.getUsername(), form.getCurrentPassword(), form.getNewPassword());
        if (updatedUserOpt.isPresent()) {
            customUserDetails.setUser(updatedUserOpt.get());
            redirectAttributes.addFlashAttribute("successMessage", "パスワードが正常に更新されました。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "現在のパスワードが正しくないか、新しいパスワードが現在のものと同じです。");
        }
        return "redirect:/mypage/password";
    }


    @PostMapping("/mypage/uploadIcon")
    @ResponseBody 
    public ResponseEntity<?> uploadIcon(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUser().getUserId();
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResponseDto(false, "ファイルが空です。"));
        }
        try {
            String newIconPath = userAccountService.saveProfileIcon(file, userId, profileUploadDir);
            
            User updatedUser = userAccountService.updateUserIcon(userId, newIconPath);
            userDetails.setUser(updatedUser); 
            return ResponseEntity.ok(new IconResponseDto(true, "アイコンが更新されました。", newIconPath));
        } catch (Exception e) {
            System.err.println("Icon Upload Error for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResponseDto(false, "アイコンの保存中にサーバーエラーが発生しました。"));
        }
    }
    
    @PostMapping("/save-color")
    public String saveUserColor(@RequestParam String color, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            String userId = userDetails.getUser().getUserId();
            userAccountService.updateUserTeamColor(userId, color);
            userDetails.getUser().setTeamColor(color);
        }
        return "redirect:/photopin";
    }

    // --- JSONレスポンス用内部クラス ---
    @Data
    private static class ResponseDto {
        public boolean success;
        public String message;
        public ResponseDto(boolean success, String message) { this.success = success; this.message = message; }
    }

    @Data
    private static class IconResponseDto extends ResponseDto {
        public String iconPath;
        public IconResponseDto(boolean success, String message, String iconPath) { 
            super(success, message); 
            this.iconPath = iconPath; 
        }
    }
}