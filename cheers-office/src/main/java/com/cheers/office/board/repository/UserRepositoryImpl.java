package com.cheers.office.board.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class UserRepositoryImpl implements UserRepository {

    @Value("${app.user-file-path}")
    private String userFilePath;
    private List<User> users;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    private void loadUsers() {
        File file = new File(userFilePath);
        if (file.exists() && file.length() > 0) {
            try {
                User[] userArray = objectMapper.readValue(file, User[].class);
                users = new ArrayList<>(List.of(userArray));
            } catch (IOException e) {
                System.err.println("Failed to load users from " + userFilePath + ": " + e.getMessage());
                users = new ArrayList<>();
            }
        } else {
            users = new ArrayList<>();
        }
    }

    @PreDestroy
    private void saveUsers() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(userFilePath), users);
        } catch (IOException e) {
            System.err.println("Failed to save users to " + userFilePath + ": " + e.getMessage());
        }
    }

    @Override
    public Optional<User> findByMailAddress(String mailAddress) {
        return users.stream()
                .filter(user -> user.getMailAddress() != null && user.getMailAddress().equals(mailAddress))
                .findFirst();
    }

    // ★findByUserId は @Override を付けて定義
    @Override
    public Optional<User> findByUserId(String userId) {
        return users.stream()
                .filter(user -> user.getUserId() != null && user.getUserId().equals(userId))
                .findFirst();
    }
    
    // ★findById はインターフェースから削除したので、実装も不要

    @Override
    public User save(User user) {
        users.add(user);
        saveUsers();
        return user;
    }

    @Override
    public User update(User updatedUser) {
        Optional<User> existingUserOpt = findByUserId(updatedUser.getUserId());
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            existingUser.setUserName(updatedUser.getUserName());
            existingUser.setMailAddress(updatedUser.getMailAddress());
            existingUser.setGroup(updatedUser.getGroup());
            existingUser.setMyBoom(updatedUser.getMyBoom());
            existingUser.setHobby(updatedUser.getHobby());
            existingUser.setIcon(updatedUser.getIcon());
            existingUser.setStatusMessage(updatedUser.getStatusMessage());
            saveUsers();
            return existingUser;
        }
        return null;
    }

    // ★deleteByMailAddress は @Override を付けて定義
    @Override
    public void deleteByMailAddress(String mailAddress) {
        users = users.stream()
                     .filter(user -> user.getMailAddress() == null || !user.getMailAddress().equals(mailAddress))
                     .collect(Collectors.toList());
        saveUsers();
    }
    
    // ★findAll は @Override を付けて定義
    @Override
    public List<User> findAll() {
        return new ArrayList<>(users);
    }
    
    // ★deleteById は @Override を付けて定義
    @Override
    public void deleteById(String userId) {
        users = users.stream()
                     .filter(user -> !user.getUserId().equals(userId))
                     .collect(Collectors.toList());
        saveUsers();
    }
}