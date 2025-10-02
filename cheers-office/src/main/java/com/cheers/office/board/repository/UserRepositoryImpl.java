package com.cheers.office.board.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository 
// JsonFileUserRepositoryをリネームしたものとして、UserRepositoryインターフェースを実装
public class UserRepositoryImpl implements UserRepository { 

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String FILE_PATH = "src/main/resources/data/user.json";
    private final File userFile;
    private final Object fileLock = new Object();

    public UserRepositoryImpl() {
        this.userFile = new File(FILE_PATH);
        // dataフォルダが存在しない場合、作成する
        if (!userFile.getParentFile().exists()) {
            userFile.getParentFile().mkdirs();
        }
        // ファイルが存在しない場合、空のJSON配列で初期化する
        if (!userFile.exists()) {
            try {
                objectMapper.writeValue(userFile, new ArrayList<User>());
            } catch (IOException e) {
                System.err.println("user.jsonの初期化エラー: " + e.getMessage());
            }
        }
    }

    // JSONファイルからユーザーリストを読み込む (排他制御あり)
    private List<User> loadUsersFromFile() {
        synchronized (fileLock) {
            try {
                if (userFile.length() == 0) {
                    return new CopyOnWriteArrayList<>();
                }
                return objectMapper.readValue(userFile, new TypeReference<List<User>>() {});
            } catch (IOException e) {
                System.err.println("ユーザーデータ (JSON) の読み込みエラー: " + e.getMessage());
                return new CopyOnWriteArrayList<>();
            }
        }
    }

    // ユーザーリストをJSONファイルに書き込む (排他制御あり)
    private void saveUsersToFile(List<User> users) {
        synchronized (fileLock) {
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(userFile, users);
            } catch (IOException e) {
                System.err.println("ユーザーデータ (JSON) の書き込みエラー: " + e.getMessage());
            }
        }
    }

    @Override
    public List<User> findAll() {
        return loadUsersFromFile();
    }

    @Override
    public Optional<User> findById(String userId) {
        return loadUsersFromFile().stream()
                .filter(user -> user.getUserId().equals(userId))
                .findFirst();
    }

    @Override
    public Optional<User> findByMailAddress(String mailAddress) {
        return loadUsersFromFile().stream()
                .filter(user -> user.getMailAddress().equals(mailAddress))
                .findFirst();
    }

    /**
     * 新規ユーザー登録時に使用 (IDの自動生成を含む)
     */
    @Override
    public User save(User user) {
        List<User> users = loadUsersFromFile();
        
        // ユーザーIDがなければ新規生成
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            user.setUserId(UUID.randomUUID().toString());
        } else {
            // 既存ユーザーの重複防止（updateメソッドを使うため、ここではシンプルな処理）
            users.removeIf(u -> u.getUserId().equals(user.getUserId()));
        }
        
        users.add(user);
        saveUsersToFile(users);
        return user;
    }

    /**
     * ★★★ プロフィール更新メソッドの実装 ★★★
     * mailAddressでユーザーを特定し、データを上書き保存する。
     */
    @Override
    public User update(User updatedUser) {
        List<User> users = loadUsersFromFile();
        
        for (int i = 0; i < users.size(); i++) {
            User existingUser = users.get(i);
            
            // ログインID（メールアドレス）で対象ユーザーを特定
            if (existingUser.getMailAddress().equals(updatedUser.getMailAddress())) {
                
                // 【重要】パスワードとIDは更新処理では変更せず、既存の値を保持する
                updatedUser.setUserId(existingUser.getUserId()); 
                
                // リスト内のオブジェクトを新しいデータで置き換え、JSONファイルに保存
                users.set(i, updatedUser); 
                saveUsersToFile(users);
                
                // 更新されたユーザーを返す
                return updatedUser; 
            }
        }
        
        // ログインユーザーの更新でここに来るべきではないため、例外を投げます
        throw new IllegalArgumentException("User not found for update: " + updatedUser.getMailAddress());
    }

    @Override
    public void deleteById(String userId) {
        List<User> users = loadUsersFromFile();
        users.removeIf(user -> user.getUserId().equals(userId));
        saveUsersToFile(users);
    }
}