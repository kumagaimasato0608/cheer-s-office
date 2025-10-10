package com.cheers.office.board.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption; // ★★★ このimport文を追加 ★★★
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;

@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String saveProfileIcon(MultipartFile file, String userId, String uploadDir) throws IOException {
        Path uploadPath = Paths.get(uploadDir); 
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String fileName = userId + "." + fileExtension;
        
        Path filePath = uploadPath.resolve(fileName);

        // ★★★ ここを修正！ ファイルの上書きを許可するオプションを追加 ★★★
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/images/profile/" + fileName;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
    
    public User updateUserIcon(String userId, String newIconPath) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIcon(newIconPath);
            userRepository.save(user);
            return user;
        }
        throw new RuntimeException("User not found with id: " + userId);
    }
    
    public void updateUserTeamColor(String userId, String color) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setTeamColor(color);
            userRepository.save(user);
        }
    }

    public boolean updateEmail(String currentEmail, String currentPassword, String newEmail) {
        Optional<User> userOpt = userRepository.findByMailAddress(currentEmail);
        if (userRepository.findByMailAddress(newEmail).isPresent()) {
            return false;
        }
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                user.setMailAddress(newEmail);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public Optional<User> updatePassword(String email, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findByMailAddress(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(currentPassword, user.getPassword()) && !passwordEncoder.matches(newPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
}