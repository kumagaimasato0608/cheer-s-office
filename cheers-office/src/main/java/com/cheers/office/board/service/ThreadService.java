package com.cheers.office.board.service;

import java.io.File; // File をインポート
import java.io.IOException;
import java.nio.file.Files; // Files をインポート
import java.nio.file.Path; // Path をインポート
import java.nio.file.Paths; // Paths をインポート
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value; // Value をインポート
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile; // MultipartFile をインポート

import com.cheers.office.board.model.ThreadModels.Reply;
import com.cheers.office.board.model.ThreadModels.ThreadPost;
import com.cheers.office.board.repository.ThreadRepository;

@Service
public class ThreadService {

    private final ThreadRepository repo;
    private final ChatService chatService; // ★ ChatService を注入

    // ★ application.properties からアップロード先ディレクトリを取得
    @Value("${app.upload-dir.thread}")
    private String threadUploadDir;

    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm");

    // ★ コンストラクタで ChatService を受け取る
    public ThreadService(ThreadRepository repo, ChatService chatService) {
        this.repo = repo;
        this.chatService = chatService; // ★ ChatService を初期化
    }

    /** 📋 スレッド一覧（7日より古い投稿は除外） */
    public List<ThreadPost> listThreads() {
        // (変更なし)
        List<ThreadPost> all = repo.findAll();
        LocalDateTime now = LocalDateTime.now(JST);
        List<ThreadPost> valid = all.stream()
                .filter(t -> {
                    LocalDateTime created = parseTimestamp(t.timestamp);
                    return created == null || created.plusDays(7).isAfter(now);
                })
                .sorted((a, b) -> compareTimestampDesc(a.timestamp, b.timestamp))
                .collect(Collectors.toList());
        if (valid.size() != all.size()) repo.saveAll(valid);
        // ★ 注意: ThreadPost モデルクラスに imageUrl フィールドを追加し、imageBase64 を削除またはコメントアウト
        return valid;
    }

    /** 🔍 検索（タイトル・日付・ユーザー名） */
    public List<ThreadPost> search(String keywordRaw) {
        // (変更なし)
        List<ThreadPost> src = listThreads();
        if (keywordRaw == null || keywordRaw.trim().isEmpty()) return src;
        String kw = normalizeKeyword(keywordRaw);
        String dateKey = normalizeDateLike(kw);
        return src.stream().filter(t -> {
            boolean byTitle = containsIgnoreCase(t.title, kw);
            boolean byUser = containsIgnoreCase(t.userName, kw);
            boolean byDate = containsIgnoreCase(t.timestamp, dateKey);
            return byTitle || byUser || byDate;
        }).sorted((a, b) -> compareTimestampDesc(a.timestamp, b.timestamp))
          .collect(Collectors.toList());
    }

    /** 🧵 掲示板作成 (修正) */
    public ThreadPost createThread(String title, String message, boolean anonymous,
                                   String userId, String userName, String icon,
                                   String imageUrl) { // ★ 引数を imageBase64 から imageUrl に変更
        List<ThreadPost> all = repo.findAll();

        ThreadPost t = new ThreadPost();
        t.threadId = UUID.randomUUID().toString();
        t.title = title;
        t.message = message;
        t.timestamp = nowTimestamp();
        t.imageUrl = imageUrl; // ★ imageBase64 の代わりに imageUrl をセット
        t.authorId = userId;

        if (anonymous) {
            t.anonymous = true;
            t.userId = "anonymous";
            t.userName = "匿名";
            t.icon = "/images/default_icon.png";
        } else {
            t.anonymous = false;
            t.userId = userId;
            t.userName = userName;
            t.icon = icon != null ? icon : "/images/default_icon.png";
        }

        t.replies = new ArrayList<>();
        all.add(t);
        repo.saveAll(all);
        return t;
    }

    /** ↩️ 返信追加（下に積み重なる） */
    public Reply addReply(String threadId, String message, boolean anonymous,
                          String userId, String userName, String icon) {
        // (変更なし)
        List<ThreadPost> all = repo.findAll();
        for (ThreadPost t : all) {
            if (t.threadId.equals(threadId)) {
                Reply r = new Reply();
                r.replyId = UUID.randomUUID().toString();
                r.message = message;
                r.authorId = userId;
                if (anonymous) {
                    r.anonymous = true;
                    r.userId = "anonymous";
                    r.userName = "匿名";
                    r.icon = "/images/default_icon.png";
                } else {
                    r.userId = userId;
                    r.userName = userName;
                    r.icon = icon != null ? icon : "/images/default_icon.png";
                }
                r.timestamp = nowTimestamp();

                if (t.replies == null) t.replies = new ArrayList<>();
                t.replies.add(r);
                repo.saveAll(all);
                return r;
            }
        }
        throw new NoSuchElementException("thread not found");
    }

    /** 🧾 ID検索 */
    public ThreadPost findById(String threadId) {
        // ★ 注意: 返される ThreadPost に imageUrl が含まれていること
        return repo.findAll().stream()
                .filter(t -> t.threadId.equals(threadId))
                .findFirst()
                .orElse(null);
    }

    /**
     * スレッドを削除します。関連する画像ファイルも削除します。
     * @param threadId 削除するスレッドのID
     * @param currentUserId 現在ログインしているユーザーのID
     */
    public void deleteThread(String threadId, String currentUserId) throws IOException {
        List<ThreadPost> allThreads = repo.findAll();

        Optional<ThreadPost> threadToDeleteOpt = allThreads.stream()
                .filter(t -> t.threadId.equals(threadId))
                .findFirst();

        if (threadToDeleteOpt.isEmpty()) {
            throw new IOException("削除対象のスレッドが見つかりません。");
        }

        ThreadPost threadToDelete = threadToDeleteOpt.get();

        // --- 権限チェック ---
        String authorId = threadToDelete.authorId;
        boolean hasPermission = false;
        if (authorId != null) {
            if (authorId.equals(currentUserId)) {
                hasPermission = true;
            }
        } else {
             // 古いデータ形式のフォールバック
            if (threadToDelete.userId != null && threadToDelete.userId.equals(currentUserId)) {
                hasPermission = true;
            }
        }
        if (!hasPermission) {
            throw new IllegalStateException("自分のスレッドしか削除できません。");
        }

        // ★★★ 画像ファイルの削除処理を追加 ★★★
        if (threadToDelete.imageUrl != null && !threadToDelete.imageUrl.isEmpty()) {
            try {
                // imageUrl からファイルパスを特定 (例: /images/thread/filename.png -> src/main/resources/static/images/thread/filename.png)
                // このパス解決方法は環境に合わせて調整が必要
                String webPath = threadToDelete.imageUrl;
                // 先頭のスラッシュを除去し、静的リソースのルートからの相対パスにする
                String relativePath = webPath.startsWith("/") ? webPath.substring(1) : webPath;
                Path imagePath = Paths.get("src/main/resources/static", relativePath); // ベースパスを適切に設定

                if (Files.exists(imagePath)) {
                    Files.delete(imagePath);
                    System.out.println("Deleted image file: " + imagePath); // ログ出力
                } else {
                     System.out.println("Image file not found, skipping deletion: " + imagePath);
                }
            } catch (IOException e) {
                // 画像削除に失敗してもスレッド削除は続行するが、エラーログは残す
                System.err.println("Failed to delete image file: " + threadToDelete.imageUrl + " - Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        // ★★★ ここまで追加 ★★★

        // スレッドをリストから削除
        allThreads.removeIf(t -> t.threadId.equals(threadId));

        // 変更をリポジトリに書き戻す
        repo.saveAll(allThreads);
    }

    // ★★★ ChatService から移植または新規実装 ★★★
    /**
     * 画像ファイルを保存し、Webアクセス可能なURLを返す
     * @param file アップロードされたファイル
     * @param uploadDir 保存先ディレクトリパス
     * @param basePath Webアクセス用ベースパス (例: "/images/thread")
     * @return Webアクセス可能なURL
     * @throws IOException ファイル保存に失敗した場合
     */
    public String saveImage(MultipartFile file, String uploadDir, String basePath) throws IOException {
        // ChatService の saveImage メソッドをここに実装するか、ChatService を利用する
        // 以下は ChatService.saveImage の実装例 (ChatService を利用する場合は不要)

        if (file == null || file.isEmpty()) {
            throw new IOException("ファイルが空です。");
        }

        // 保存先ディレクトリ作成
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // ユニークなファイル名を生成 (UUID + 拡張子)
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        Path filePath = Paths.get(uploadDir, uniqueFilename);

        // ファイルを保存
        Files.copy(file.getInputStream(), filePath);

        // Webアクセス用のURLを返す (例: /images/thread/uuid.png)
        return basePath + "/" + uniqueFilename;

        // ChatService を使う場合:
        // return chatService.saveImage(file, uploadDir, basePath);
    }


    // ---------- Utility ----------
    private String nowTimestamp() { return LocalDateTime.now(JST).format(OUT_FMT); }
    private LocalDateTime parseTimestamp(String ts) { try { return LocalDateTime.parse(ts, OUT_FMT); } catch (Exception e) { return null; } }
    private int compareTimestampDesc(String a, String b) { LocalDateTime la = parseTimestamp(a), lb = parseTimestamp(b); if (la == null) return 1; if (lb == null) return -1; return lb.compareTo(la); }
    private String normalizeKeyword(String s) { if (s == null) return ""; return Normalizer.normalize(s, Normalizer.Form.NFKC).trim().toLowerCase(); }
    private boolean containsIgnoreCase(String target, String needle) { return target != null && target.toLowerCase().contains(needle); }
    private String normalizeDateLike(String kw) { if (kw == null) return ""; return kw.replace("年", "/").replace("月", "/").replace("日", ""); }
}