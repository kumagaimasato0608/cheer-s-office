package com.cheers.office.board.model;

import java.time.LocalDateTime;
import java.util.List; // スレッドに含まれる投稿のリスト用

// Lombokのアノテーションを使用する場合、pom.xmlにlombokの依存関係が必要です。
// @Data // Getter, Setter, toString, equals, hashCode を自動生成
// @NoArgsConstructor // 引数なしコンストラクタを自動生成
// @AllArgsConstructor // 全フィールドの引数を持つコンストラクタを自動生成

public class Thread {
    private String threadId; // スレッドの一意なID
    private String title;    // スレッドのタイトル
    private String creatorUserId; // スレッド作成者のID
    private LocalDateTime createdAt; // スレッド作成日時
    private List<Post> posts; // このスレッドに含まれる投稿のリスト (関連付け)

    // デフォルトコンストラクタ (必須)
    public Thread() {
    }

    // コンストラクタ
    public Thread(String threadId, String title, String creatorUserId, LocalDateTime createdAt, List<Post> posts) {
        this.threadId = threadId;
        this.title = title;
        this.creatorUserId = creatorUserId;
        this.createdAt = createdAt;
        this.posts = posts;
    }

    // --- GetterとSetter (Lombokを使わない場合は手動で記述) ---
    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCreatorUserId() { return creatorUserId; }
    public void setCreatorUserId(String creatorUserId) { this.creatorUserId = creatorUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<Post> getPosts() { return posts; }
    public void setPosts(List<Post> posts) { this.posts = posts; }

    @Override
    public String toString() {
        return "Thread{" +
               "threadId='" + threadId + '\'' +
               ", title='" + title + '\'' +
               ", creatorUserId='" + creatorUserId + '\'' +
               ", createdAt=" + createdAt +
               ", posts=" + (posts != null ? posts.size() : 0) +
               '}';
    }
}