package com.cheers.office.board.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.web.bind.annotation.RequestPart; // ★ これを追加
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.cheers.office.board.dto.ScoreUpdateDto;
import com.cheers.office.board.model.BonusZone;
import com.cheers.office.board.model.Comment;
import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.Location;
import com.cheers.office.board.model.Photo;
import com.cheers.office.board.model.Pinit;
import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.PinitRepository;
import com.cheers.office.board.repository.UserRepository;
import com.cheers.office.board.service.UserAccountService;

/**
 * ===============================================================
 * 📍 PinIt コントローラ（フォトピン機能）
 * ---------------------------------------------------------------
 * ・ピン投稿・削除・コメント・リアクション・スコア管理
 * ・Tomcat 再起動後も JSON 永続化でデータ保持
 * ・WebSocket でスコアリアルタイム更新
 * ===============================================================
 */
@Controller
public class PinitController {

    private final PinitRepository pinitRepository;
    private final UserAccountService userAccountService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    // 📍 ボーナスゾーン設定
    private final List<BonusZone> bonusZones = List.of(
            new BonusZone("東京タワー", 35.658581, 139.745433, 200, 500),
            new BonusZone("皇居", 35.685175, 139.7528, 500, 1000),
            new BonusZone("東京スカイツリー", 35.710063, 139.8107, 200, 500)
    );

    // ✅ 修正版: photopin の upload パスを EC2 環境に対応
    @Value("${app.upload-dir.photopin:/home/ec2-user/cheers-data/images/photopins}")
    private String photopinUploadDir;

    public PinitController(
            PinitRepository pinitRepository,
            UserAccountService userAccountService,
            SimpMessagingTemplate messagingTemplate,
            UserRepository userRepository) {
        this.pinitRepository = pinitRepository;
        this.userAccountService = userAccountService;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    // 現在のシーズン (例: 2025-10)
    private String getCurrentSeason() {
        return YearMonth.now().toString();
    }

    // === ページ表示 ===
    @GetMapping("/Pinit")
    public String showPhotoPinPage(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null &&
                (userDetails.getUser().getTeamColor() == null || userDetails.getUser().getTeamColor().isEmpty())) {
            model.addAttribute("showColorModal", true);
        }
        return "pinit";
    }

    // === チームカラー設定 ===
    @PostMapping("/Pinit/save-color")
    public String saveTeamColor(@RequestParam("color") String color,
                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null && color != null && !color.isEmpty()) {
            User user = userDetails.getUser();
            user.setTeamColor(color);
            userAccountService.updateUser(user);
            calculateAndBroadcastScores();
        }
        return "redirect:/Pinit";
    }

    // === チュートリアル完了API ===
    @PostMapping("/api/user/completeTutorial")
    @ResponseBody
    public ResponseEntity<Void> completeTutorial(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User user = userDetails.getUser();
        if (!user.isTutorialSeen()) {
            user.setTutorialSeen(true);
            userAccountService.updateUser(user);
        }
        return ResponseEntity.ok().build();
    }

    // === 全ピン取得 ===
    @GetMapping("/api/photopins")
    @ResponseBody
    public ResponseEntity<List<Pinit>> getAllPhotoPins(@RequestParam(required = false) String season) {
        String targetSeason = (season != null && !season.isEmpty()) ? season : getCurrentSeason();
        List<Pinit> pins = pinitRepository.findAll().stream()
                .filter(pin -> targetSeason.equals(pin.getSeason()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(pins);
    }

    // === シーズン一覧 ===
    @GetMapping("/api/photopins/seasons")
    @ResponseBody
    public ResponseEntity<Set<String>> getAvailableSeasons() {
        Set<String> seasons = pinitRepository.findAll().stream()
                .map(Pinit::getSeason)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(seasons);
    }

    // === 現在のスコア取得 ===
    @GetMapping("/api/scores")
    @ResponseBody
    public ScoreUpdateDto getPinItScores() {
        return calculateScoresInternal();
    }

    // === リアクション（いいね/行きたい/見た） ===
    @PostMapping("/api/photopins/{pinId}/react")
    @ResponseBody
    public ResponseEntity<?> toggleReaction(@PathVariable String pinId,
                                            @RequestParam String type, // ★ @RequestParam はここで使われているのでOK
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ログインが必要です。");

        String currentUserId = userDetails.getUser().getUserId();
        Optional<Pinit> pinOpt = pinitRepository.findById(pinId);
        if (pinOpt.isEmpty()) return ResponseEntity.notFound().build();

        Pinit pin = pinOpt.get();
        if (pin.getReactions() == null)
            pin.setReactions(new HashMap<>());

        List<String> usersReacted = pin.getReactions().computeIfAbsent(type, k -> new ArrayList<>());
        if (usersReacted.contains(currentUserId))
            usersReacted.remove(currentUserId);
        else
            usersReacted.add(currentUserId);

        Pinit savedPin = pinitRepository.savePin(pin);
        calculateAndBroadcastScores();
        return ResponseEntity.ok(savedPin);
    }

    // === 新規ピン登録 (★ 修正箇所) ===
    @PostMapping("/api/photopins")
    @ResponseBody
    public ResponseEntity<?> createPhotoPin(
            @RequestPart("title") String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart("latitude") String latitudeStr,   // ★ Stringで受け取る
            @RequestPart("longitude") String longitudeStr, // ★ Stringで受け取る
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        // ★ Stringをdoubleに手動で変換
        double latitude;
        double longitude;
        try {
            latitude = Double.parseDouble(latitudeStr);
            longitude = Double.parseDouble(longitudeStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("緯度経度の形式が不正です。");
        }
        // ★ 変換ここまで

        if (userDetails == null || file.isEmpty())
            return ResponseEntity.badRequest().build();

        User user = userDetails.getUser();
        if (user.getTeamColor() == null || user.getTeamColor().isEmpty())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("ピンを置く前にチームカラーを選択してください。");

        String currentUserId = user.getUserId();
        String currentSeason = getCurrentSeason();

        // クールタイム制限
        LocalDateTime lastPinTime = user.getLastPinTimestamp();
        if (lastPinTime != null) {
            long hours = ChronoUnit.HOURS.between(lastPinTime, LocalDateTime.now());
            if (hours < 1) {
                long minutesToWait = 60 - ChronoUnit.MINUTES.between(lastPinTime, LocalDateTime.now());
                return ResponseEntity.badRequest().body("次のピンまであと " + minutesToWait + " 分お待ちください。");
            }
        }

        // 距離制限
        boolean tooClose = pinitRepository.findAll().stream()
                .filter(p -> currentSeason.equals(p.getSeason()) && currentUserId.equals(p.getCreatedBy()))
                .anyMatch(p -> distance(latitude, longitude, p.getLocation().getLatitude(), p.getLocation().getLongitude()) < 100);
        if (tooClose)
            return ResponseEntity.badRequest().body("他のピンから100m以内には設置できません。");

        Pinit newPin = new Pinit();
        newPin.setPinId(UUID.randomUUID().toString());
        newPin.setTitle(title);
        newPin.setDescription(description);
        newPin.setLocation(new Location(latitude, longitude));
        newPin.setCreatedBy(currentUserId);
        newPin.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        newPin.setSeason(currentSeason);

        // ボーナス判定
        for (BonusZone zone : bonusZones) {
            if (distance(latitude, longitude, zone.latitude(), zone.longitude()) <= zone.radius()) {
                newPin.setBonusPoints(zone.points());
                break;
            }
        }

        // ファイル保存処理
        try {
            Path uploadPath = Paths.get(photopinUploadDir);
            if (!Files.exists(uploadPath))
                Files.createDirectories(uploadPath);

            String fileName = newPin.getPinId() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            // 既存ファイルがあっても上書き
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Photo photo = new Photo();
            photo.setPhotoId(UUID.randomUUID().toString());
            photo.setImageUrl("/images/photopins/" + fileName);
            photo.setUploadedBy(currentUserId);
            photo.setUploadedDate(newPin.getCreatedDate());
            newPin.getPhotos().add(photo);

            Pinit savedPin = pinitRepository.savePin(newPin);

            user.setLastPinTimestamp(LocalDateTime.now());
            userAccountService.updateUser(user);
            calculateAndBroadcastScores();

            return ResponseEntity.status(HttpStatus.CREATED).body(savedPin);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("画像保存に失敗しました。");
        }
    }

    // === ピン更新 ===
    @PutMapping("/api/photopins/{pinId}")
    @ResponseBody
    public ResponseEntity<Pinit> updatePin(@PathVariable String pinId,
                                           @RequestBody Pinit updatedPin, // ★ ここはJSONなので @RequestBody のまま
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        Optional<Pinit> pinOpt = pinitRepository.findById(pinId);
        if (pinOpt.isEmpty()) return ResponseEntity.notFound().build();

        Pinit pin = pinOpt.get();
        if (!pin.getCreatedBy().equals(userDetails.getUser().getUserId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        pin.setTitle(updatedPin.getTitle());
        pin.setDescription(updatedPin.getDescription());
        pinitRepository.savePin(pin);
        calculateAndBroadcastScores();
        return ResponseEntity.ok(pin);
    }

    // === ピン削除 ===
    @DeleteMapping("/api/photopins/{pinId}")
    @ResponseBody
    public ResponseEntity<Void> deletePin(@PathVariable String pinId,
                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        Optional<Pinit> pinOpt = pinitRepository.findById(pinId);
        if (pinOpt.isEmpty()) return ResponseEntity.notFound().build();

        Pinit pin = pinOpt.get();
        if (!pin.getCreatedBy().equals(userDetails.getUser().getUserId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (pin.getPhotos() != null) {
            for (Photo photo : pin.getPhotos()) {
                try {
                    String fileName = photo.getImageUrl().substring(photo.getImageUrl().lastIndexOf("/") + 1);
                    Files.deleteIfExists(Paths.get(photopinUploadDir, fileName));
                } catch (Exception e) {
                    System.err.println("写真削除失敗: " + e.getMessage());
                }
            }
        }

        pinitRepository.deleteById(pinId);
        calculateAndBroadcastScores();
        return ResponseEntity.noContent().build();
    }

    // === コメント取得 ===
    @GetMapping("/api/photopins/{pinId}/comments")
    @ResponseBody
    public ResponseEntity<List<Comment>> getComments(@PathVariable String pinId) {
        return pinitRepository.findById(pinId)
                .map(pin -> ResponseEntity.ok(pin.getComments()))
                .orElse(ResponseEntity.ok(Collections.emptyList()));
    }

    // === コメント投稿 ===
    @PostMapping("/api/photopins/{pinId}/comments")
    @ResponseBody
    public ResponseEntity<Comment> addComment(@PathVariable String pinId,
                                              @RequestBody Comment newComment, // ★ ここはJSONなので @RequestBody のまま
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        Optional<Pinit> pinOpt = pinitRepository.findById(pinId);
        if (pinOpt.isEmpty()) return ResponseEntity.notFound().build();

        newComment.setCommentId(UUID.randomUUID().toString());
        newComment.setPinId(pinId);
        newComment.setUserId(userDetails.getUser().getUserId());
        newComment.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        Pinit pin = pinOpt.get();
        pin.getComments().add(newComment);
        pinitRepository.savePin(pin);
        calculateAndBroadcastScores();
        return ResponseEntity.status(HttpStatus.CREATED).body(newComment);
    }

    // === スコア計算ロジック ===
    private ScoreUpdateDto calculateScoresInternal() {
        String currentSeason = getCurrentSeason();
        Map<String, String> userTeamMap = userRepository.findAll().stream()
                .filter(u -> u.getTeamColor() != null)
                .collect(Collectors.toMap(User::getUserId, User::getTeamColor, (a, b) -> a));

        List<Pinit> pins = pinitRepository.findAll().stream()
                .filter(p -> currentSeason.equals(p.getSeason()))
                .sorted(Comparator.comparing(Pinit::getCreatedDate))
                .collect(Collectors.toList());

        Map<String, String> grid = new HashMap<>();
        final double CELL_SIZE = 5.0;
        final double RANGE = 50.0;
        final double LAT_M = 111320.0;
        int STEPS = (int) Math.ceil(RANGE / CELL_SIZE);

        for (Pinit p : pins) {
            String color = userTeamMap.get(p.getCreatedBy());
            if (color == null) continue;
            Location loc = p.getLocation();
            double lngM = 40075000.0 * Math.cos(Math.toRadians(loc.getLatitude())) / 360.0;
            long latStep = Math.round(loc.getLatitude() * LAT_M / CELL_SIZE);
            long lngStep = Math.round(loc.getLongitude() * lngM / CELL_SIZE);

            for (int i = -STEPS; i <= STEPS; i++)
                for (int j = -STEPS; j <= STEPS; j++)
                    grid.put((latStep + i) + "_" + (lngStep + j), color);
        }

        Map<String, Integer> scores = new HashMap<>(Map.of("red", 0, "blue", 0, "yellow", 0));
        grid.values().forEach(c -> scores.merge(c, 1, Integer::sum));
        pins.forEach(p -> {
            String color = userTeamMap.get(p.getCreatedBy());
            if (color != null && scores.containsKey(color))
                scores.merge(color, p.getBonusPoints(), Integer::sum);
        });
        return new ScoreUpdateDto(scores.get("red"), scores.get("blue"), scores.get("yellow"));
    }

    // === スコアブロードキャスト ===
    private void calculateAndBroadcastScores() {
        ScoreUpdateDto update = calculateScoresInternal();
        messagingTemplate.convertAndSend("/topic/scores", update);
        System.out.println("スコア更新送信: " + update);
    }

    // === 距離計算 ===
    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6378137.0;
        lat1 = Math.toRadians(lat1);
        lon1 = Math.toRadians(lon1);
        lat2 = Math.toRadians(lat2);
        lon2 = Math.toRadians(lon2);
        double dLon = lon2 - lon1;
        double dLat = lat2 - lat1;
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dLon / 2), 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}