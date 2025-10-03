package com.cheers.office.board.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequestMapping("/mypage")
public class MypageController {

    private final UserRepository userRepository;

    // パスワード/メールアドレス関連の処理を分離したため、PasswordEncoderは不要
    public MypageController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * プロフィール編集画面の表示 (GET /mypage)
     * mypage.html を返します。
     */
    @GetMapping
    public String mypage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
            // ログイン中のユーザー情報をModelに渡す
            model.addAttribute("user", customUserDetails.getUser());
        } else {
            return "redirect:/login"; // 認証されていない場合はログイン画面へ
        }

        return "mypage"; 
    }

    /**
     * プロフィール情報（ユーザー名、グループ、趣味、一言など）の更新処理
     * POST /mypage/updateProfile
     */
    @PostMapping("/updateProfile")
    public String updateProfile(
            @RequestParam("userName") String userName,
            @RequestParam("group") String group,
            @RequestParam("myBoom") String myBoom,
            @RequestParam("hobby") String hobby,
            @RequestParam("icon") String icon,
            @RequestParam("statusMessage") String statusMessage,
            RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            redirectAttributes.addFlashAttribute("errorMessage", "セッションが無効です。再度ログインしてください。");
            return "redirect:/login";
        }

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        User currentUser = customUserDetails.getUser();

        // フォームから受け取った値でUserオブジェクトを作成し、更新対象外の項目は既存の値を引き継ぐ
        User updatedUser = new User();
        updatedUser.setUserName(userName);
        updatedUser.setGroup(group);
        updatedUser.setMyBoom(myBoom);
        updatedUser.setHobby(hobby);
        updatedUser.setIcon(icon);
        updatedUser.setStatusMessage(statusMessage);

        // 変更されないフィールドは元の値を保持させる
        updatedUser.setMailAddress(currentUser.getMailAddress());
        updatedUser.setPassword(currentUser.getPassword()); 
        updatedUser.setUserId(currentUser.getUserId()); 

        // UserRepositoryで更新を実行
        User resultUser = userRepository.update(updatedUser); 

        if (resultUser != null) { 
            // SecurityContextの情報を更新 (セッション内の古いユーザー情報を置き換える)
            customUserDetails.setUser(resultUser);
            
            redirectAttributes.addFlashAttribute("successMessage", "プロフィール情報が更新されました。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "プロフィールの更新に失敗しました。");
        }
        
        return "redirect:/mypage"; 
    }
    
    // ★★★ 以前あった以下のメソッドは、PasswordChangeControllerに移動したため削除しました ★★★
    // - showPasswordChangeForm(Model model)
    // - updateEmail(...)
    // - updatePassword(...)
}