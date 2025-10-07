package com.cheers.office.board.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cheers.office.board.dto.PhotoUploadForm; // ★DTOをインポート
import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.PhotoPin; // ★モデルをインポート
import com.cheers.office.board.service.PhotopinService;

@Controller
@RequestMapping("/photopin")
public class PhotopinController {
    
    private final PhotopinService photopinService; // ★PhotopinServiceを仮定

    public PhotopinController(PhotopinService photopinService) {
        this.photopinService = photopinService;
    }

    // A. 画面表示 (GET /photopin)
    @GetMapping
    public String showPhotoPinPage(Model model) {
        // 画面表示に必要な初期設定などがあればここでModelに追加
        return "photopin"; // photopin.html を返す
    }
    
    // B. 写真アップロード (POST /photopin/upload)
    @PostMapping("/upload")
    public String uploadPhoto(
            @ModelAttribute PhotoUploadForm form,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        if (form.getFile().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "ファイルを選択してください。");
            return "redirect:/photopin";
        }
        
        try {
            // Serviceに処理を委譲 (ファイル保存、DB登録)
            photopinService.savePhotoPin(form, userDetails.getUser().getUserId());
            redirectAttributes.addFlashAttribute("successMessage", "写真が地図にピンされました！");
        } catch (Exception e) {
            System.err.println("Photo Upload Error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "写真のアップロード中にエラーが発生しました。");
        }

        return "redirect:/photopin";
    }
    
    /**
     * C. 地図データ取得API (GET /api/photopins)
     * JavaScriptにピン情報をJSONで提供する
     */
    @GetMapping("/api/pins") 
    @ResponseBody // JSONを返す
    public List<PhotoPin> getPhotoPinsApi() {
        // Serviceからすべてのピンデータを取得してそのまま返す
        return photopinService.findAllPhotoPins();
    }
}