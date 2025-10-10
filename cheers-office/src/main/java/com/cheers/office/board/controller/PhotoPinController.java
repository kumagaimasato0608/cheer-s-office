package com.cheers.office.board.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.cheers.office.board.model.Comment;
import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.Location;
import com.cheers.office.board.model.Photo;
import com.cheers.office.board.model.PhotoPin;
import com.cheers.office.board.repository.PhotoPinRepository;
import com.cheers.office.board.service.UserAccountService;

@Controller
public class PhotoPinController {

    private final PhotoPinRepository photoPinRepository;
    private final UserAccountService userAccountService;

    @Value("${app.upload-dir.photopin:src/main/resources/static/images/photopins}")
    private String photopinUploadDir;

    public PhotoPinController(PhotoPinRepository photoPinRepository, UserAccountService userAccountService) {
        this.photoPinRepository = photoPinRepository;
        this.userAccountService = userAccountService;
    }

    // (showPhotoPinPage, getAllPhotoPins, createPhotoPin, updatePin, deletePin は変更なし)
    @GetMapping("/photopin")
    public String showPhotoPinPage(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) { if (userDetails != null && (userDetails.getUser().getTeamColor() == null || userDetails.getUser().getTeamColor().isEmpty())) { model.addAttribute("showColorModal", true); } return "photopin"; }
    @GetMapping("/api/photopins")
    @ResponseBody
    public ResponseEntity<List<PhotoPin>> getAllPhotoPins() { List<PhotoPin> pins = photoPinRepository.findAll(); return ResponseEntity.ok(pins); }
    @PostMapping("/api/photopins")
    @ResponseBody
    public ResponseEntity<PhotoPin> createPhotoPin(@RequestParam("title") String title, @RequestParam(value = "description", required = false) String description, @RequestParam("latitude") double latitude, @RequestParam("longitude") double longitude, @RequestParam("file") MultipartFile file, @AuthenticationPrincipal CustomUserDetails customUserDetails) { if (customUserDetails == null || file.isEmpty()) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); } PhotoPin newPin = new PhotoPin(); newPin.setPinId(UUID.randomUUID().toString()); newPin.setTitle(title); newPin.setDescription(description); newPin.setLocation(new Location(latitude, longitude)); newPin.setCreatedBy(customUserDetails.getUser().getUserId()); newPin.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); try { Path uploadPath = Paths.get(photopinUploadDir); if (!Files.exists(uploadPath)) { Files.createDirectories(uploadPath); } String fileName = newPin.getPinId() + "_" + file.getOriginalFilename(); Path filePath = uploadPath.resolve(fileName); Files.copy(file.getInputStream(), filePath); Photo newPhoto = new Photo(); newPhoto.setPhotoId(UUID.randomUUID().toString()); newPhoto.setImageUrl("/images/photopins/" + fileName); newPhoto.setUploadedBy(customUserDetails.getUser().getUserId()); newPhoto.setUploadedDate(newPin.getCreatedDate()); newPin.getPhotos().add(newPhoto); PhotoPin savedPin = photoPinRepository.savePin(newPin); return ResponseEntity.status(HttpStatus.CREATED).body(savedPin); } catch (IOException e) { e.printStackTrace(); return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); } }
    @PutMapping("/api/photopins/{pinId}")
    @ResponseBody
    public ResponseEntity<PhotoPin> updatePin(@PathVariable String pinId, @RequestBody PhotoPin updatedPinData, @AuthenticationPrincipal CustomUserDetails userDetails) { Optional<PhotoPin> pinOpt = photoPinRepository.findById(pinId); if (pinOpt.isEmpty()) { return ResponseEntity.notFound().build(); } PhotoPin pin = pinOpt.get(); if (!pin.getCreatedBy().equals(userDetails.getUser().getUserId())) { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); } pin.setTitle(updatedPinData.getTitle()); pin.setDescription(updatedPinData.getDescription()); photoPinRepository.savePin(pin); return ResponseEntity.ok(pin); }
    @DeleteMapping("/api/photopins/{pinId}")
    @ResponseBody
    public ResponseEntity<Void> deletePin(@PathVariable String pinId, @AuthenticationPrincipal CustomUserDetails userDetails) { Optional<PhotoPin> pinOpt = photoPinRepository.findById(pinId); if (pinOpt.isEmpty()) { return ResponseEntity.notFound().build(); } if (!pinOpt.get().getCreatedBy().equals(userDetails.getUser().getUserId())) { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); } photoPinRepository.deleteById(pinId); return ResponseEntity.noContent().build(); }


    @GetMapping("/api/photopins/{pinId}/comments")
    @ResponseBody
    public ResponseEntity<List<Comment>> getComments(@PathVariable String pinId) {
        Optional<PhotoPin> pinOpt = photoPinRepository.findById(pinId);
        if (pinOpt.isPresent()) {
            return ResponseEntity.ok(pinOpt.get().getComments());
        }
        return ResponseEntity.ok(Collections.emptyList());
    }

    /**
     * 特定のピンにコメントを追加する
     */
    @PostMapping("/api/photopins/{pinId}/comments")
    @ResponseBody
    public ResponseEntity<Comment> addComment(@PathVariable String pinId,
                                            @RequestBody Comment newComment,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Optional<PhotoPin> pinOpt = photoPinRepository.findById(pinId);
        if (pinOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // ★★★ 既存のComment.javaのフィールドに合わせて設定 ★★★
        newComment.setCommentId(UUID.randomUUID().toString());
        newComment.setPinId(pinId); // ピンのIDを設定
        newComment.setUserId(userDetails.getUser().getUserId()); // 作成者ID
        newComment.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); // タイムスタンプ

        PhotoPin pin = pinOpt.get();
        pin.getComments().add(newComment);
        photoPinRepository.savePin(pin);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(newComment);
    }
}