package com.cheers.office.board.service;

import java.io.IOException;
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

import org.springframework.stereotype.Service;

import com.cheers.office.board.model.ThreadModels.Reply;
import com.cheers.office.board.model.ThreadModels.ThreadPost;
import com.cheers.office.board.repository.ThreadRepository;

@Service
public class ThreadService {

    private final ThreadRepository repo;
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");
    private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm");

    public ThreadService(ThreadRepository repo) {
        this.repo = repo;
    }

    /** 📋 スレッド一覧（7日より古い投稿は除外） */
    public List<ThreadPost> listThreads() {
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
        return valid;
    }

    /** 🔍 検索（タイトル・日付・ユーザー名） */
    public List<ThreadPost> search(String keywordRaw) {
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

    /** 🧵 掲示板作成 */
    public ThreadPost createThread(String title, String message, boolean anonymous,
                                     String userId, String userName, String icon,
                                     String imageBase64) {
        List<ThreadPost> all = repo.findAll();

        ThreadPost t = new ThreadPost();
        t.threadId = UUID.randomUUID().toString();
        t.title = title;
        t.message = message;
        t.timestamp = nowTimestamp();
        t.imageBase64 = imageBase64;
        t.authorId = userId; // 投稿者のIDを保存

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
        List<ThreadPost> all = repo.findAll();
        for (ThreadPost t : all) {
            if (t.threadId.equals(threadId)) {
                Reply r = new Reply();
                r.replyId = UUID.randomUUID().toString();
                r.message = message;
                r.authorId = userId; // 投稿者のIDを保存
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
        return repo.findAll().stream()
                .filter(t -> t.threadId.equals(threadId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * スレッドを削除します。
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

        // ▼▼▼ この部分を修正 ▼▼▼
        // --- 権限チェック ---
        String authorId = threadToDelete.authorId;
        boolean hasPermission = false;

        // 1. 新しいデータ形式 (authorId が存在する) の場合
        if (authorId != null) {
            if (authorId.equals(currentUserId)) {
                hasPermission = true;
            }
        } 
        // 2. 古いデータ形式 (authorId が null) の場合、userId で代用チェック
        else {
            // 匿名投稿ではなく、かつuserIdが一致する場合に権限ありとみなす
            if (threadToDelete.userId != null && threadToDelete.userId.equals(currentUserId)) {
                hasPermission = true;
            }
        }

        // 最終的な権限チェック
        if (!hasPermission) {
            throw new IllegalStateException("自分のスレッドしか削除できません。");
        }
        // ▲▲▲ ここまで修正 ▲▲▲

        // スレッドをリストから削除
        allThreads.removeIf(t -> t.threadId.equals(threadId));

        // 変更をリポジトリに書き戻す
        repo.saveAll(allThreads);
    }

    // ---------- Utility ----------
    private String nowTimestamp() { return LocalDateTime.now(JST).format(OUT_FMT); }
    private LocalDateTime parseTimestamp(String ts) { try { return LocalDateTime.parse(ts, OUT_FMT); } catch (Exception e) { return null; } }
    private int compareTimestampDesc(String a, String b) { LocalDateTime la = parseTimestamp(a), lb = parseTimestamp(b); if (la == null) return 1; if (lb == null) return -1; return lb.compareTo(la); }
    private String normalizeKeyword(String s) { if (s == null) return ""; return Normalizer.normalize(s, Normalizer.Form.NFKC).trim().toLowerCase(); }
    private boolean containsIgnoreCase(String target, String needle) { return target != null && target.toLowerCase().contains(needle); }
    private String normalizeDateLike(String kw) { if (kw == null) return ""; return kw.replace("年", "/").replace("月", "/").replace("日", ""); }
}