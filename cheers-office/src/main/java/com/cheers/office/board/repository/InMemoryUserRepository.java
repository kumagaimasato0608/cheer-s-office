package com.cheers.office.board.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.User;

@Repository
public class InMemoryUserRepository implements UserRepository { // ★UserRepositoryインターフェースを実装

    private final Map<String, User> users = new ConcurrentHashMap<>();

    // コンストラクタ（CheersOfficeApplicationで初期ユーザーを追加するため空でOK）
    public InMemoryUserRepository() {
        // 必要であればここで初期ユーザーを追加することも可能
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public Optional<User> findByMailAddress(String mailAddress) {
        return users.values().stream()
                .filter(u -> u.getMailAddress().equals(mailAddress))
                .findFirst();
    }
    
    @Override
    public Optional<User> findById(String userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public User save(User user) {
        // 新規ユーザーの場合、IDを設定
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            user.setUserId(UUID.randomUUID().toString());
        }
        users.put(user.getUserId(), user);
        return user;
    }
}