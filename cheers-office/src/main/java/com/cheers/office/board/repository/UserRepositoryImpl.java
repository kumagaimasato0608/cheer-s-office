package com.cheers.office.board.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final ObjectMapper objectMapper;
    private final File userFile;
    // CopyOnWriteArrayListはスレッドセーフなキャッシュとして維持します
    private final CopyOnWriteArrayList<User> users; 

    public UserRepositoryImpl(ObjectMapper objectMapper, @Value("${app.user-file-path:src/main/resources/data/users.json}") String userFilePath) {
        this.objectMapper = objectMapper;
        this.userFile = new File(userFilePath);
        this.users = new CopyOnWriteArrayList<>();
        // 起動時の初期ロードは維持
        loadUsers(); 
    }

    /**
     * JSONファイルからユーザーデータを読み込み、メモリ上のキャッシュを更新する
     * (このメソッドは、findAll()とsave()の両方から呼ばれ、キャッシュを最新の状態に保つ)
     */
    private synchronized void loadUsers() {
        if (userFile.exists() && userFile.length() > 0) {
            try {
                // ファイルからデータを読み込む
                List<User> loadedUsers = objectMapper.readValue(userFile, new TypeReference<List<User>>() {});
                
                // ★★★ キャッシュをクリアし、新しいデータで上書き ★★★
                this.users.clear();
                this.users.addAll(loadedUsers);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * メモリ上のデータをJSONファイルに書き出す
     */
    private void saveUsersToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(userFile, users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ★★★ 修正箇所: ユーザー一覧ページに入るたびにキャッシュを読み込み直す ★★★
     */
    @Override
    public List<User> findAll() {
        // ページにアクセスされるたび（/api/usersが呼ばれるたび）にファイルを読み込み直す
        loadUsers(); 
        return new ArrayList<>(users);
    }

    @Override
    public Optional<User> findById(String userId) {
        // findAll()で最新データがロードされているため、キャッシュから参照
        return users.stream()
                .filter(user -> user.getUserId().equals(userId))
                .findFirst();
    }

    @Override
    public Optional<User> findByMailAddress(String mailAddress) {
        // findAll()で最新データがロードされているため、キャッシュから参照
        return users.stream()
                .filter(user -> user.getMailAddress() != null && user.getMailAddress().equals(mailAddress))
                .findFirst();
    }

    @Override
    public User save(User user) {
        Optional<Integer> indexOpt = IntStream.range(0, users.size())
                .filter(i -> users.get(i).getUserId().equals(user.getUserId()))
                .boxed()
                .findFirst();

        if (indexOpt.isPresent()) {
            users.set(indexOpt.get(), user);
        } else {
            users.add(user);
        }
        
        // ファイルへの保存（マイページからの更新）
        saveUsersToFile(); 
        
        // ★★★ 修正箇所: save後もキャッシュは最新なので特別な操作は不要 (ただし、findAllで再ロードされる) ★★★
        
        return user;
    }

    @Override
    public void deleteById(String userId) {
        users.removeIf(user -> user.getUserId().equals(userId));
        saveUsersToFile();
    }

    @Override
    public User update(User user) {
        // saveメソッドが更新処理も兼ねているので、そのまま呼び出す
        return save(user);
    }
}