package com.cheers.office.board.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID; // UUIDのためのインポートを追加
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final ObjectMapper objectMapper;
    private final File userFile;
    private final CopyOnWriteArrayList<User> users; // スレッドセーフなリストに変更

    // 💡 コンストラクタでObjectMapperとファイルパスをDIで受け取る方式は優秀です
    public UserRepositoryImpl(ObjectMapper objectMapper, @Value("${app.user-file-path:src/main/resources/data/user.json}") String userFilePath) {
        this.objectMapper = objectMapper;
        this.userFile = new File(userFilePath);
        this.users = new CopyOnWriteArrayList<>();
        loadUsers();
    }
    
    // ... loadUsers(), saveUsers() メソッドは省略（前述のコードと同一） ...
    
    // 【再掲】JSONファイルの読み込み（loadUsers()）
    private void loadUsers() {
        if (userFile.exists() && userFile.length() > 0) {
            try {
                List<User> loadedUsers = objectMapper.readValue(userFile, new TypeReference<List<User>>() {});
                this.users.clear();
                this.users.addAll(loadedUsers);
            } catch (IOException e) {
                System.err.println("Failed to load users from file: " + e.getMessage());
            }
        }
    }

    // 【再掲】JSONファイルへの書き込み（saveUsers()）
    private void saveUsers() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(userFile, users);
        } catch (IOException e) {
            System.err.println("Failed to save users to file: " + e.getMessage());
        }
    }


    // --- 抽象メソッドの実装 ---

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users); // defensive copy
    }

    @Override
    public Optional<User> findById(String userId) {
        return users.stream()
                .filter(user -> user.getUserId().equals(userId))
                .findFirst();
    }
    
    // 💡 UserRepositoryインターフェースに findByMailAddress があると仮定して実装
    @Override
    public Optional<User> findByMailAddress(String mailAddress) {
        return users.stream()
                .filter(user -> user.getMailAddress() != null && user.getMailAddress().equals(mailAddress))
                .findFirst();
    }

    // 💡 戻り値の型を User に修正し、新規IDの割り当てロジックを追加
    @Override
    public User save(User user) {
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
             user.setUserId(UUID.randomUUID().toString()); // 新規IDを割り当て
        }
        users.add(user);
        saveUsers();
        return user; // 保存したユーザーオブジェクトを返す
    }
    @Override
    public User update(User updatedUser) {
        // userIdが一致するユーザーを見つけ、更新
        for (int i = 0; i < users.size(); i++) {
            User existingUser = users.get(i);
            
            // 💡 修正点: userIdでの一意な特定のみを行う
            if (existingUser.getUserId().equals(updatedUser.getUserId())) {
                
                // 【重要】パスワードを既存の値で上書きして保持する
                // 💡 Service層でこの処理を行うのが理想的ですが、リポジトリ側で安全を担保します
                updatedUser.setPassword(existingUser.getPassword());
                
                // 既存のユーザーを更新（リスト内の要素を置き換え）
                users.set(i, updatedUser); 
                saveUsers(); // ファイルに保存
                return updatedUser;
            }
        }
        
        // ユーザーIDに一致するユーザーが見つからなかった場合
        throw new IllegalArgumentException("User not found for update: ID " + updatedUser.getUserId());
    }
    
    // 💡 UserRepositoryインターフェースに deleteById があると仮定して実装
    @Override
    public void deleteById(String userId) {
        boolean removed = users.removeIf(user -> user.getUserId().equals(userId));
        if (removed) {
            saveUsers();
        }
    }
}