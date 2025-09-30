package com.cheers.office.board.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList; // Threadコンストラクタで使用

import org.springframework.stereotype.Service; // ★★★ このimportを追加 ★★★

import com.cheers.office.board.model.Post;
import com.cheers.office.board.model.Thread;
import com.cheers.office.board.repository.BoardRepository;

@Service // ★★★ このアノテーションを追加 ★★★
public class BoardService {

    private final BoardRepository boardRepository;
    private final CustomUserDetailsService userDetailsService; // ユーザー名取得のため

    // コンストラクタインジェクション
    public BoardService(BoardRepository boardRepository, CustomUserDetailsService userDetailsService) {
        this.boardRepository = boardRepository;
        this.userDetailsService = userDetailsService;
    }

    // 全スレッドを取得 (新しい順)
    public List<Thread> findAllThreads() {
        List<Thread> threads = boardRepository.findAllThreads();
        // 必要に応じてソートロジックを追加
        return threads;
    }

    // 指定IDのスレッドを取得
    public Optional<Thread> findThreadById(String threadId) {
        return boardRepository.findThreadById(threadId);
    }

    // 新規スレッドと最初の投稿を作成
    public Thread createNewThread(String title, String content, String userId) {
        String threadId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        Post firstPost = new Post(
            UUID.randomUUID().toString(), // postId
            threadId, // threadId
            userId,   // 投稿者ID
            content,  // 投稿内容
            now       // 投稿日時
        );

        Thread newThread = new Thread(
            threadId,   // スレッドID
            title,      // タイトル
            userId,     // 作成者ID
            now,        // 作成日時
            new CopyOnWriteArrayList<>(List.of(firstPost)) // 最初の投稿をリストに追加
        );

        return boardRepository.saveThread(newThread);
    }

    // 既存スレッドに投稿を追加 (リプライ)
    public Optional<Thread> addPostToThread(String threadId, String content, String userId) {
        Optional<Thread> threadOptional = boardRepository.findThreadById(threadId);
        threadOptional.ifPresent(thread -> {
            Post newPost = new Post(
                UUID.randomUUID().toString(), // postId
                threadId, // threadId
                userId,   // 投稿者ID
                content,  // 投稿内容
                LocalDateTime.now() // 投稿日時
            );
            boardRepository.addPostToThread(threadId, newPost);
        });
        return threadOptional;
    }

    // 投稿者の表示名を解決するヘルパーメソッド
    public String getDisplayName(String userId) {
        // TODO: 実際はUserモデルからdisplayNameを取得するロジックを実装
        return userId;
    }

    // (オプション) 古いスレッドを削除するロジック (Spring Schedulerと連携)
    public void deleteOldThreads(LocalDateTime cutoffDate) {
        // このメソッドの実装は後回し
    }
}