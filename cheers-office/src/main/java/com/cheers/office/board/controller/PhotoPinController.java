package com.cheers.office.board.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.Location;
import com.cheers.office.board.model.Photo;
import com.cheers.office.board.model.PhotoPin;
import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.PhotoPinRepository;
import com.cheers.office.board.repository.UserRepository;

@Controller
public class PhotoPinController {

    private final PhotoPinRepository photoPinRepository;
    private final UserRepository userRepository;

    @Value("${app.upload-dir:src/main/resources/static/uploads}")
    private String uploadDir;

    public PhotoPinController(PhotoPinRepository photoPinRepository, UserRepository userRepository) {
        this.photoPinRepository = photoPinRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/photopin")
    public String showPhotoPinPage() {
        return "photopin";
    }

    @PostMapping("/api/photopins")
    @ResponseBody
    public ResponseEntity<PhotoPin> createPhotoPin(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        if (customUserDetails == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        PhotoPin newPin = new PhotoPin();
        newPin.setTitle(title);
        newPin.setDescription(description);
        newPin.setLocation(new Location(latitude, longitude));
        newPin.setCreatedBy(customUserDetails.getUser().getUserId());
        newPin.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            Photo newPhoto = new Photo();
            newPhoto.setPhotoId(UUID.randomUUID().toString());
            newPhoto.setImageUrl("/uploads/" + fileName);
            newPhoto.setComment("");
            newPhoto.setUploadedBy(customUserDetails.getUser().getUserId());
            newPhoto.setUploadedDate(newPin.getCreatedDate());

            newPin.getPhotos().add(newPhoto);

            PhotoPin savedPin = photoPinRepository.savePin(newPin);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPin);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/ranking/pins")
    @ResponseBody
    public List<PinRankingDto> getPinRanking() {
        List<PhotoPin> allPins = photoPinRepository.findAllPins();
        Map<String, User> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));

        Map<String, Long> pinCounts = allPins.stream()
                .collect(Collectors.groupingBy(PhotoPin::getCreatedBy, Collectors.counting()));

        return pinCounts.entrySet().stream()
                .map(entry -> {
                    User user = userMap.get(entry.getKey());
                    if (user == null) return null;
                    return new PinRankingDto(user.getUserName(), user.getIcon(), entry.getValue());
                })
                .filter(dto -> dto != null)
                .sorted(Comparator.comparingLong(PinRankingDto::getPinCount).reversed())
                .limit(3)
                .collect(Collectors.toList());
    }

    @GetMapping("/api/photopins") 
    @ResponseBody 
    public List<PhotoPin> getAllPhotoPins() { 
        return photoPinRepository.findAllPins(); 
    }

    @GetMapping("/api/photopins/{id}") 
    @ResponseBody 
    public ResponseEntity<PhotoPin> getPhotoPinById(@PathVariable String id) { 
        return photoPinRepository.findPinById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build()); 
    }

    @PutMapping("/api/photopins/{id}") 
    @ResponseBody 
    public ResponseEntity<PhotoPin> updatePhotoPin(@PathVariable String id, @RequestBody PhotoPin photoPin, @AuthenticationPrincipal CustomUserDetails customUserDetails) { 
        if (customUserDetails == null) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }
        Optional<PhotoPin> existingPinOptional = photoPinRepository.findPinById(id);
        if (existingPinOptional.isEmpty()) { return ResponseEntity.notFound().build(); }
        PhotoPin existingPin = existingPinOptional.get();
        if (!existingPin.getCreatedBy().equals(customUserDetails.getUser().getUserId())) { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }
        existingPin.setTitle(photoPin.getTitle());
        existingPin.setDescription(photoPin.getDescription());
        PhotoPin updatedPin = photoPinRepository.savePin(existingPin);
        return ResponseEntity.ok(updatedPin);
    }

    @DeleteMapping("/api/photopins/{id}") 
    @ResponseBody 
    public ResponseEntity<Void> deletePhotoPin(@PathVariable String id, @AuthenticationPrincipal CustomUserDetails customUserDetails) { 
        if (customUserDetails == null) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }
        Optional<PhotoPin> existingPinOptional = photoPinRepository.findPinById(id);
        if (existingPinOptional.isEmpty()) { return ResponseEntity.notFound().build(); }
        PhotoPin existingPin = existingPinOptional.get();
        if (!existingPin.getCreatedBy().equals(customUserDetails.getUser().getUserId())) { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }
        photoPinRepository.deletePin(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/photopins/users") 
    @ResponseBody 
    public List<User> getAllUsers() { 
        return userRepository.findAll().stream()
                .map(user -> {
                    User safeUser = new User();
                    safeUser.setUserId(user.getUserId());
                    safeUser.setUserName(user.getUserName());
                    safeUser.setIcon(user.getIcon());
                    return safeUser;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/api/photopins/{pinId}/photos") 
    @ResponseBody 
    public ResponseEntity<PhotoPin> addPhotoToPin(@PathVariable String pinId, @RequestParam("file") MultipartFile file, @RequestParam(value = "comment", required = false) String comment, @AuthenticationPrincipal CustomUserDetails customUserDetails) { 
        if (customUserDetails == null) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }
        Optional<PhotoPin> existingPinOptional = photoPinRepository.findPinById(pinId);
        if (existingPinOptional.isEmpty()) { return ResponseEntity.notFound().build(); }
        PhotoPin existingPin = existingPinOptional.get();
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) { Files.createDirectories(uploadPath); }
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            Photo newPhoto = new Photo();
            newPhoto.setPhotoId(UUID.randomUUID().toString());
            newPhoto.setImageUrl("/uploads/" + fileName);
            newPhoto.setComment(comment != null ? comment : "");
            newPhoto.setUploadedBy(customUserDetails.getUser().getUserId());
            newPhoto.setUploadedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            existingPin.getPhotos().add(newPhoto);
            PhotoPin updatedPin = photoPinRepository.savePin(existingPin);
            return ResponseEntity.ok(updatedPin);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static class PinRankingDto {
        private String userName;
        private String userIcon;
        private long pinCount;
        public PinRankingDto(String userName, String userIcon, long pinCount) { this.userName = userName; this.userIcon = userIcon; this.pinCount = pinCount; }
        public String getUserName() { return userName; }
        public String getUserIcon() { return userIcon; }
        public long getPinCount() { return pinCount; }
    }
}