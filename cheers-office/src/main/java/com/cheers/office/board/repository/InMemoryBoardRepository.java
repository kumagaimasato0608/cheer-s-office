package com.cheers.office.board.repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors; // import java.util.stream.Collectors; の記述は上部にまとめる

import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.Post;
import com.cheers.office.board.model.Thread; // ★★★ この行を追加 ★★★

@Repository
public class InMemoryBoardRepository implements BoardRepository {

    // スレッドIDをキーとしてスレッドを格納
    private final Map<String, Thread> threads = new ConcurrentHashMap<>();

    public InMemoryBoardRepository() {
        // 初期データ (テスト用)
        String threadId1 = UUID.randomUUID().toString();
        String threadId2 = UUID.randomUUID().toString();

        // スレッド1
        Thread thread1 = new Thread(threadId1, "掲示板機能の提案", "u001_auto", LocalDateTime.now().minusMinutes(30), new CopyOnWriteArrayList<>());
        Post post1_1 = new Post(UUID.randomUUID().toString(), threadId1, "u001_auto", "掲示板機能についてアイデアを出し合いましょう。", LocalDateTime.now().minusMinutes(29));
        Post post1_2 = new Post(UUID.randomUUID().toString(), threadId1, "u999_test", "賛成です！初期のデータ構造はこれで良いと思います。", LocalDateTime.now().minusMinutes(20));
        thread1.getPosts().add(post1_1);
        thread1.getPosts().add(post1_2);
        threads.put(threadId1, thread1);

        // スレッド2
        Thread thread2 = new Thread(threadId2, "今日のランチどうする？", "u900_test", LocalDateTime.now().minusHours(2), new CopyOnWriteArrayList<>());
        Post post2_1 = new Post(UUID.randomUUID().toString(), threadId2, "u900_test", "お昼ごはん、何食べますか？", LocalDateTime.now().minusHours(1).minusMinutes(50));
        Post post2_2 = new Post(UUID.randomUUID().toString(), thread2.getThreadId(), "u001_auto", "ラーメンとかどうですか？", LocalDateTime.now().minusMinutes(45));
        thread2.getPosts().add(post2_1);
        thread2.getPosts().add(post2_2);
        threads.put(threadId2, thread2);
    }

    @Override
    public List<Thread> findAllThreads() {
        // 新しいスレッドが上に来るようにソート
        return threads.values().stream()
                .sorted(Comparator.comparing(Thread::getCreatedAt).reversed())
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
    }

    @Override
    public Optional<Thread> findThreadById(String threadId) {
        return Optional.ofNullable(threads.get(threadId));
    }

    @Override
    public Thread saveThread(Thread thread) {
        // 新規スレッドの場合、IDと作成日時を設定
        if (thread.getThreadId() == null || thread.getThreadId().isEmpty()) {
            thread.setThreadId(UUID.randomUUID().toString());
        }
        if (thread.getCreatedAt() == null) {
            thread.setCreatedAt(LocalDateTime.now());
        }
        // 投稿リストがnullなら初期化
        if (thread.getPosts() == null) {
            thread.setPosts(new CopyOnWriteArrayList<>());
        }
        threads.put(thread.getThreadId(), thread);
        return thread;
    }

    @Override
    public void addPostToThread(String threadId, Post post) {
        findThreadById(threadId).ifPresent(thread -> {
            // 投稿IDとタイムスタンプを設定
            if (post.getPostId() == null || post.getPostId().isEmpty()) {
                post.setPostId(UUID.randomUUID().toString());
            }
            if (post.getTimestamp() == null) {
                post.setTimestamp(LocalDateTime.now());
            }
            thread.getPosts().add(post);
        });
    }

    @Override
    public void deleteThread(String threadId) {
        threads.remove(threadId);
    }

    @Override
    public Optional<Post> findPostById(String postId) {
        for (Thread thread : threads.values()) {
            for (Post post : thread.getPosts()) {
                if (post.getPostId().equals(postId)) {
                    return Optional.of(post);
                }
            }
        }
        return Optional.empty();
    }
}