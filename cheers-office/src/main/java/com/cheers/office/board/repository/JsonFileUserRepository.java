package com.cheers.office.board.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList; // スレッドセーフなリスト

import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository // Springにリポジトリとして認識させるアノテーション
public class JsonFileUserRepository implements UserRepository { // UserRepositoryインターフェースを実装

    private final ObjectMapper objectMapper = new ObjectMapper();
    // JSONファイルパス: プロジェクトルートの data/user.json
    private static final String FILE_PATH = "src/main/resources/data/user.json";
    private final File userFile; // ファイルオブジェクト
    private final Object fileLock = new Object(); // ファイル書き込み時の排他制御用

    public JsonFileUserRepository() {
        // コンストラクタでファイルオブジェクトを初期化
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

    // JSONファイルからユーザーリストを読み込む
    private List<User> loadUsersFromFile() {
        synchronized (fileLock) { // 排他制御
            try {
                if (userFile.length() == 0) { // ファイルが空の場合は空リストを返す
                    return new CopyOnWriteArrayList<>();
                }
                // JSONファイルから List<User> を読み込む
                return objectMapper.readValue(userFile, new TypeReference<List<User>>() {});
            } catch (IOException e) {
                System.err.println("ユーザーデータ (JSON) の読み込みエラー: " + e.getMessage());
                return new CopyOnWriteArrayList<>();
            }
        }
    }

    // ユーザーリストをJSONファイルに書き込む
    private void saveUsersToFile(List<User> users) {
        synchronized (fileLock) { // 排他制御
            try {
                // List<User> を JSONファイルに整形して書き込む
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

    @Override
    public User save(User user) {
        List<User> users = loadUsersFromFile();
        
        // ユーザーIDがなければ新規生成
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            user.setUserId(UUID.randomUUID().toString());
        } else {
            // 既存ユーザーをリストから削除（更新のため）
            users.removeIf(u -> u.getUserId().equals(user.getUserId()));
        }
        
        users.add(user); // 新規または更新ユーザーを追加
        saveUsersToFile(users); // ファイルに書き込み
        return user;
    }

    @Override
    public void deleteById(String userId) {
        List<User> users = loadUsersFromFile();
        users.removeIf(user -> user.getUserId().equals(userId)); // 指定IDのユーザーを削除
        saveUsersToFile(users); // ファイルに書き込み
    }
}