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
    private final CopyOnWriteArrayList<User> users;

    public UserRepositoryImpl(ObjectMapper objectMapper, 
            @Value("${app.user-file-path}") String userFilePath) {
        this.objectMapper = objectMapper;
        this.userFile = new File(userFilePath);
        this.users = new CopyOnWriteArrayList<>();
        loadUsers(); // 起動時にロード
    }

    private synchronized void loadUsers() {
        if (userFile.exists() && userFile.length() > 0) {
            try {
                List<User> loadedUsers = objectMapper.readValue(userFile, new TypeReference<List<User>>() {});
                users.clear();
                users.addAll(loadedUsers);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveUsersToFile() {
        try {
            // フォルダが存在しない場合は作成
            File parentDir = userFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(userFile, users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<User> findAll() {
        loadUsers();
        return new ArrayList<>(users);
    }

    @Override
    public Optional<User> findById(String userId) {
        return users.stream()
                .filter(user -> user.getUserId().equals(userId))
                .findFirst();
    }

    @Override
    public Optional<User> findByMailAddress(String mailAddress) {
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

        saveUsersToFile();
        return user;
    }

    @Override
    public void deleteById(String userId) {
        users.removeIf(user -> user.getUserId().equals(userId));
        saveUsersToFile();
    }

    @Override
    public User update(User user) {
        return save(user);
    }
}
