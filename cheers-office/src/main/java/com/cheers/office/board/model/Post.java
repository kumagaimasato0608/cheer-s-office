package com.cheers.office.board.model;

import java.time.LocalDateTime;

// ★Post.java (Thread.java が参照するクラス)
public class Post {
    private String postId;    // 投稿の一意なID
    private String threadId;  // どのスレッドに属するかのID
    private String userId;    // 投稿者のID
    private String content;   // 投稿内容
    private LocalDateTime timestamp; // 投稿日時

    // デフォルトコンストラクタ (必須)
    public Post() {
    }

    // コンストラクタ (InMemoryBoardRepositoryで使用)
    public Post(String postId, String threadId, String userId, String content, LocalDateTime timestamp) {
        this.postId = postId;
        this.threadId = threadId;
        this.userId = userId;
        this.content = content;
        this.timestamp = timestamp;
    }

    // --- GetterとSetter (InMemoryBoardRepositoryで必須) ---
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}