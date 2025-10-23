package com.cheers.office.board.controller;

import java.io.IOException; // IOException をインポート
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value; // Value をインポート
import org.springframework.http.HttpStatus; // HttpStatus をインポート
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile; // MultipartFile をインポート

import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.ThreadModels.Reply;
import com.cheers.office.board.model.ThreadModels.ThreadPost;
import com.cheers.office.board.service.ThreadService;
// ChatService を使う場合: import com.cheers.office.board.service.ChatService;

@RestController
@RequestMapping("/api/thread")
public class ThreadController {

    private final ThreadService service;
    // ChatService を使う場合: private final ChatService chatService;

    // ★ 追加: application.properties からアップロード先ディレクトリを取得
    @Value("${app.upload-dir.thread}") // application.properties に app.upload-dir.thread=/path/to/upload を追加
    private String threadUploadDir;

    // コンストラクタを修正 (ChatService を使う場合)
    public ThreadController(ThreadService service /* , ChatService chatService */) {
        this.service = service;
        // this.chatService = chatService;
    }

    /** 📋 スレッド一覧 */
    @GetMapping("/list")
    public List<ThreadPost> list() {
        return service.listThreads();
    }

    /** 🔍 検索機能 */
    @GetMapping("/search")
    public List<ThreadPost> search(@RequestParam(required = false) String keyword) {
        return service.search(keyword);
    }

    // ★★★ 追加: 画像アップロード用API ★★★
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadThreadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "ファイルが空です"));
        }
        try {
            // ThreadService に画像保存メソッド (saveImage) が実装されている前提
            // もしくは ChatService を使う -> chatService.saveImage(...)
            // 第3引数はWebアクセス用のベースパス (例: /images/thread)
            String imageUrl = service.saveImage(file, threadUploadDir, "/images/thread");
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (IOException e) {
            e.printStackTrace(); // エラーログ
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "ファイルのアップロード中にエラーが発生しました"));
        } catch (Exception e) {
             // saveImage メソッドが未実装の場合などの汎用エラー
             e.printStackTrace();
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                  .body(Map.of("error", "画像処理中にエラーが発生しました"));
        }
    }
    // ★★★ ここまで追加 ★★★


    /** 🧵 掲示板スレッド作成 (修正) */
    @PostMapping("/create")
    public ThreadPost create(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> req) {

        String title = (String) req.get("title");
        String message = (String) req.get("message");
        boolean anonymous = req.get("anonymous") != null && (Boolean) req.get("anonymous");
        // ★ 修正: imageBase64 の代わりに imageUrl を受け取る
        String imageUrl = (String) req.get("imageUrl"); // キー名を imageUrl に変更

        // ★ 修正: ThreadService の createThread メソッドも imageUrl を受け取るように変更が必要
        return service.createThread(
                title, message, anonymous,
                user != null ? user.getUser().getUserId() : "anonymous",
                user != null ? user.getUser().getUserName() : "匿名",
                user != null ? user.getUser().getIcon() : "/images/default_icon.png",
                imageUrl // imageBase64 の代わりに imageUrl を渡す
        );
    }

    /** 💬 返信追加 */
    @PostMapping("/{threadId}/reply")
    public Reply addReply(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String threadId,
            @RequestBody Map<String, Object> req) {
        // (返信には画像がないので、このメソッドは変更なし)
        String message = (String) req.get("message");
        boolean anonymous = req.get("anonymous") != null && (Boolean) req.get("anonymous");

        return service.addReply(
                threadId, message, anonymous,
                user != null ? user.getUser().getUserId() : "anonymous",
                user != null ? user.getUser().getUserName() : "匿名",
                user != null ? user.getUser().getIcon() : "/images/default_icon.png"
        );
    }

    /** 🧾 スレッド単体取得 */
    @GetMapping("/{threadId}")
    public ThreadPost getThread(@PathVariable String threadId) {
        // ★ 注意: ThreadService.findById が返す ThreadPost に imageUrl が含まれるように修正が必要
        return service.findById(threadId);
    }

    /** 🗑️ スレッド削除 */
    @DeleteMapping("/{threadId}")
    public ResponseEntity<?> deleteThread(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String threadId) {
        // (このメソッドは変更なし、ただし Service 層で画像ファイルも削除するロジックが必要になるかも)
        if (user == null) {
            return ResponseEntity.status(401).body("ログインが必要です。");
        }

        try {
            service.deleteThread(threadId, user.getUser().getUserId());
            return ResponseEntity.ok().body("スレッドを削除しました。");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("スレッドの削除に失敗しました。");
        }
    }
}

// ThreadViewController は変更なし
@Controller
class ThreadViewController {
    @GetMapping("/thread")
    public String view() {
        return "thread";
    }
}