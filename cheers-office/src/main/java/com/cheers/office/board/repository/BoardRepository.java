package com.cheers.office.board.repository;

import java.util.List;
import java.util.Optional;

import com.cheers.office.board.model.Post;
import com.cheers.office.board.model.Thread; // ★★★ このimportを追加 ★★★

public interface BoardRepository {
    // 全スレッドを取得 (新しいもの順など、ソートは実装で考慮)
    List<Thread> findAllThreads();

    // 指定されたIDのスレッドを取得
    Optional<Thread> findThreadById(String threadId);

    // 新規スレッドを保存（最初の投稿と同時に作成）
    Thread saveThread(Thread thread);

    // 指定されたスレッドに投稿を追加
    void addPostToThread(String threadId, Post post);

    // 指定されたスIDを持つスレッドのリストを削除
    void deleteThread(String threadId);

    // (任意) 投稿IDで投稿を検索するメソッド
    Optional<Post> findPostById(String postId);
}