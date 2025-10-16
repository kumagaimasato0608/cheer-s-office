package com.cheers.office.board.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

import com.cheers.office.board.dto.ScoreUpdateDto;
import com.cheers.office.board.model.BonusZone;
import com.cheers.office.board.model.Comment;
import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.Location;
import com.cheers.office.board.model.Photo;
import com.cheers.office.board.model.PhotoPin;
import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.PhotoPinRepository;
import com.cheers.office.board.repository.UserRepository;
import com.cheers.office.board.service.UserAccountService;

@Controller
public class PhotoPinController {

    private final PhotoPinRepository photoPinRepository;
    private final UserAccountService userAccountService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    private final List<BonusZone> bonusZones = List.of(
        new BonusZone("東京タワー", 35.658581, 139.745433, 200, 500),
        new BonusZone("皇居", 35.685175, 139.7528, 500, 1000),
        new BonusZone("東京スカイツリー", 35.710063, 139.8107, 200, 500)
    );

    @Value("${app.upload-dir.photopin:src/main/resources/static/images/photopins}")
    private String photopinUploadDir;

    public PhotoPinController(PhotoPinRepository photoPinRepository, 
                              UserAccountService userAccountService, 
                              SimpMessagingTemplate messagingTemplate,
                              UserRepository userRepository) {
        this.photoPinRepository = photoPinRepository;
        this.userAccountService = userAccountService;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    private String getCurrentSeason() {
        return YearMonth.now().toString();
    }
    
    @GetMapping("/photopin")
    public String showPhotoPinPage(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) { 
        if (userDetails != null && (userDetails.getUser().getTeamColor() == null || userDetails.getUser().getTeamColor().isEmpty())) { 
            model.addAttribute("showColorModal", true); 
        } 
        return "photopin"; 
    }
    
    @GetMapping("/api/photopins")
    @ResponseBody
    public ResponseEntity<List<PhotoPin>> getAllPhotoPins(@RequestParam(required = false) String season) {
        String targetSeason = (season != null && !season.isEmpty()) ? season : getCurrentSeason();
        List<PhotoPin> pinsForSeason = photoPinRepository.findAll().stream()
                .filter(pin -> targetSeason.equals(pin.getSeason()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(pinsForSeason);
    }
    
    @GetMapping("/api/photopins/seasons")
    @ResponseBody
    public ResponseEntity<Set<String>> getAvailableSeasons() {
        Set<String> seasons = photoPinRepository.findAll().stream()
                .map(PhotoPin::getSeason)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toSet());
        return ResponseEntity.ok(seasons);
    }

    @PostMapping("/api/photopins")
    @ResponseBody
    public ResponseEntity<?> createPhotoPin(@RequestParam("title") String title, @RequestParam(value = "description", required = false) String description, @RequestParam("latitude") double latitude, @RequestParam("longitude") double longitude, @RequestParam("file") MultipartFile file, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (customUserDetails == null || file.isEmpty()) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); }
        
        // チームカラーチェック
        if (customUserDetails.getUser().getTeamColor() == null || customUserDetails.getUser().getTeamColor().isEmpty()) {
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body("ピンを置く前に、チームカラーを選択してください。");
        }

        String currentUserId = customUserDetails.getUser().getUserId();
        String currentSeason = getCurrentSeason();

        LocalDateTime lastPinTime = customUserDetails.getUser().getLastPinTimestamp();
        if (lastPinTime != null) {
            long hoursSinceLastPin = ChronoUnit.HOURS.between(lastPinTime, LocalDateTime.now());
            if (hoursSinceLastPin < 1) {
                long minutesToWait = 60 - ChronoUnit.MINUTES.between(lastPinTime, LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("次のピンを置くまで、あと " + minutesToWait + " 分お待ちください。");
            }
        }

        List<PhotoPin> myPins = photoPinRepository.findAll().stream()
                .filter(pin -> currentSeason.equals(pin.getSeason()) && currentUserId.equals(pin.getCreatedBy()))
                .collect(Collectors.toList());
        for (PhotoPin myPin : myPins) {
            double dist = distance(latitude, longitude, myPin.getLocation().getLatitude(), myPin.getLocation().getLongitude());
            if (dist < 100) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("自分の他のピンから100m以内に新しいピンを置くことはできません。");
            }
        }
        
        PhotoPin newPin = new PhotoPin();
        newPin.setPinId(UUID.randomUUID().toString());
        newPin.setTitle(title);
        newPin.setDescription(description);
        newPin.setLocation(new Location(latitude, longitude));
        newPin.setCreatedBy(currentUserId);
        newPin.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        newPin.setSeason(currentSeason);

        for (BonusZone zone : bonusZones) {
            double dist = distance(latitude, longitude, zone.latitude(), zone.longitude());
            if (dist <= zone.radius()) {
                newPin.setBonusPoints(zone.points());
                break; 
            }
        }

        try {
            Path uploadPath = Paths.get(photopinUploadDir);
            if (!Files.exists(uploadPath)) { Files.createDirectories(uploadPath); }
            String fileName = newPin.getPinId() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            Photo newPhoto = new Photo();
            newPhoto.setPhotoId(UUID.randomUUID().toString());
            newPhoto.setImageUrl("/images/photopins/" + fileName);
            newPhoto.setUploadedBy(currentUserId);
            newPhoto.setUploadedDate(newPin.getCreatedDate());
            newPin.getPhotos().add(newPhoto);
            PhotoPin savedPin = photoPinRepository.savePin(newPin);
            
            customUserDetails.getUser().setLastPinTimestamp(LocalDateTime.now());
            userAccountService.updateUser(customUserDetails.getUser());
            
            calculateAndBroadcastScores();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPin);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Optional<PhotoPin> findPinByIdForCurrentSeason(String pinId) {
        return photoPinRepository.findById(pinId)
            .filter(pin -> getCurrentSeason().equals(pin.getSeason()));
    }
    
    @PutMapping("/api/photopins/{pinId}")
    @ResponseBody
    public ResponseEntity<PhotoPin> updatePin(@PathVariable String pinId, @RequestBody PhotoPin updatedPinData, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Optional<PhotoPin> pinOpt = findPinByIdForCurrentSeason(pinId);
        if (pinOpt.isEmpty()) { return ResponseEntity.notFound().build(); }
        PhotoPin pin = pinOpt.get();
        if (!pin.getCreatedBy().equals(userDetails.getUser().getUserId())) { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }
        pin.setTitle(updatedPinData.getTitle());
        pin.setDescription(updatedPinData.getDescription());
        photoPinRepository.savePin(pin);
        return ResponseEntity.ok(pin);
    }

    // ★★★ 修正: ファイル削除ロジックを追加 ★★★
    @DeleteMapping("/api/photopins/{pinId}")
    @ResponseBody
    public ResponseEntity<Void> deletePin(@PathVariable String pinId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Optional<PhotoPin> pinOpt = findPinByIdForCurrentSeason(pinId);
        if (pinOpt.isEmpty()) { return ResponseEntity.notFound().build(); }
        
        PhotoPin pinToDelete = pinOpt.get();

        // 権限チェック
        if (!pinToDelete.getCreatedBy().equals(userDetails.getUser().getUserId())) { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }
        
        // ファイルシステムの画像削除処理
        if (pinToDelete.getPhotos() != null) {
            for (Photo photo : pinToDelete.getPhotos()) {
                try {
                    // /images/photopins/ のプレフィックスを削除し、ファイル名のみを取得
                    String imageUrl = photo.getImageUrl();
                    String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                    
                    // パスを構築: photopinUploadDir + fileName
                    Path filePath = Paths.get(photopinUploadDir, fileName);

                    // ファイルが存在する場合のみ削除 (エラーをスローしないよう Files.deleteIfExists を使用)
                    Files.deleteIfExists(filePath);
                    System.out.println("✅ Pin Photo Deleted: " + fileName);
                    
                } catch (IOException | StringIndexOutOfBoundsException e) {
                    System.err.println("Failed to delete photo file: " + e.getMessage());
                }
            }
        }
        
        photoPinRepository.deleteById(pinId);
        
        calculateAndBroadcastScores();
        
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/api/photopins/{pinId}/comments")
    @ResponseBody
    public ResponseEntity<List<Comment>> getComments(@PathVariable String pinId) {
        Optional<PhotoPin> pinOpt = photoPinRepository.findById(pinId);
        if (pinOpt.isPresent()) { return ResponseEntity.ok(pinOpt.get().getComments()); }
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PostMapping("/api/photopins/{pinId}/comments")
    @ResponseBody
    public ResponseEntity<Comment> addComment(@PathVariable String pinId, @RequestBody Comment newComment, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Optional<PhotoPin> pinOpt = findPinByIdForCurrentSeason(pinId);
        if (pinOpt.isEmpty()) { return ResponseEntity.notFound().build(); }
        newComment.setCommentId(UUID.randomUUID().toString());
        newComment.setPinId(pinId);
        newComment.setUserId(userDetails.getUser().getUserId());
        newComment.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        PhotoPin pin = pinOpt.get();
        pin.getComments().add(newComment);
        photoPinRepository.savePin(pin);
        return ResponseEntity.status(HttpStatus.CREATED).body(newComment);
    }

    private ScoreUpdateDto calculateScoresInternal() {
        // calculateScoresInternal() は、初期スコア取得APIとブロードキャストロジックの両方から使用されます。
        
        String currentSeason = getCurrentSeason();
        
        Map<String, String> userTeamColorMap = userRepository.findAll().stream()
            .filter(user -> user.getTeamColor() != null) 
            .collect(Collectors.toMap(User::getUserId, User::getTeamColor, (c1, c2) -> c1));

        List<PhotoPin> pins = photoPinRepository.findAll().stream()
            .filter(pin -> currentSeason.equals(pin.getSeason()))
            .sorted(Comparator.comparing(PhotoPin::getCreatedDate))
            .collect(Collectors.toList());
        Map<String, String> gridState = new HashMap<>();
        
        // スコア計算の定数 (home.htmlのJSと一致)
        final double CELL_SIZE_METERS = 5.0; 
        final double INFLUENCE_RANGE_METERS = 50.0;
        final int MAX_TILE_STEPS = (int) Math.ceil(INFLUENCE_RANGE_METERS / CELL_SIZE_METERS); // 10

        final double METERS_PER_DEGREE_LAT = 111320.0; 
        
        for (PhotoPin pin : pins) {
            String teamColor = userTeamColorMap.get(pin.getCreatedBy());
            if (teamColor == null) continue; 
            
            Location loc = pin.getLocation();
            
            double initialMetersPerLng = 40075000.0 * Math.cos(Math.toRadians(loc.getLatitude())) / 360.0;
            
            // 原点からの距離をマス数（整数）に変換
            long latStepCenter = Math.round(loc.getLatitude() * METERS_PER_DEGREE_LAT / CELL_SIZE_METERS);
            long lngStepCenter = Math.round(loc.getLongitude() * initialMetersPerLng / CELL_SIZE_METERS);
            
            int maxSteps = MAX_TILE_STEPS; // 10

            for (int i = -maxSteps; i <= maxSteps; i++) {
                for (int j = -maxSteps; j <= maxSteps; j++) {
                    
                    long tileLatStep = latStepCenter + i;
                    long tileLngStep = lngStepCenter + j;

                    String cellId = tileLatStep + "_" + tileLngStep;
                    
                    gridState.put(cellId, teamColor);
                }
            }
        }
        
        Map<String, Integer> scores = new HashMap<>();
        scores.put("red", 0); scores.put("blue", 0); scores.put("yellow", 0);
        gridState.values().forEach(color -> {
            if (scores.containsKey(color)) {
                scores.merge(color, 1, Integer::sum);
            }
        });
        pins.forEach(pin -> {
            if (pin.getBonusPoints() > 0) {
                String teamColor = userTeamColorMap.get(pin.getCreatedBy());
                if (teamColor != null && scores.containsKey(teamColor)) {
                    scores.merge(teamColor, pin.getBonusPoints(), Integer::sum);
                }
            }
        });
        
        return new ScoreUpdateDto(scores.get("red"), scores.get("blue"), scores.get("yellow"));
    }

    private void calculateAndBroadcastScores() {
        // calculateAndBroadcastScoresはブロードキャスト専用とし、スコア計算はcalculateScoresInternal()を使う
        ScoreUpdateDto scoreUpdate = calculateScoresInternal();
        messagingTemplate.convertAndSend("/topic/scores", scoreUpdate);
        System.out.println("スコア更新を送信: " + scoreUpdate);
    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6378137.0; lat1 = Math.toRadians(lat1); lon1 = Math.toRadians(lon1); lat2 = Math.toRadians(lat2); lon2 = Math.toRadians(lon2);
        double dLon = lon2 - lon1; double dLat = lat2 - lat1;
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}