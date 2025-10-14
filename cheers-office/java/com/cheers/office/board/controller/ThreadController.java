package com.cheers.office.board.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.ThreadModels.Reply;
import com.cheers.office.board.model.ThreadModels.ThreadPost;
import com.cheers.office.board.service.ThreadService;

/**
 * 掲示板コントローラ（REST + ページ表示）
 */
@RestController
@RequestMapping("/api/thread")
public class ThreadController {

    private final ThreadService service;

    public ThreadController(ThreadService service) {
        this.service = service;
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

    /** 🧵 掲示板スレッド作成（画像付き対応） */
    @PostMapping("/create")
    public ThreadPost create(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> req) {

        String title = (String) req.get("title");
        String message = (String) req.get("message");
        boolean anonymous = req.get("anonymous") != null && (Boolean) req.get("anonymous");
        String imageBase64 = (String) req.getOrDefault("imageBase64", "");

        return service.createThread(
                title, message, anonymous,
                user != null ? user.getUser().getUserId() : "anonymous",
                user != null ? user.getUser().getUserName() : "匿名",
                user != null ? user.getUser().getIcon() : "/images/default_icon.png",
                imageBase64
        );
    }

    /** 💬 返信追加 */
    @PostMapping("/{threadId}/reply")
    public Reply addReply(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String threadId,
            @RequestBody Map<String, Object> req) {

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
        return service.findById(threadId);
    }
}

/* ==============================
   📄 画面遷移コントローラ
   ============================== */
@Controller
class ThreadViewController {
    @GetMapping("/thread")
    public String view() {
        return "thread";
    }
}
