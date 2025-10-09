package com.cheers.office.board.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;
import com.cheers.office.util.PasswordUtil;

@Service
public class UserAccountService {

    private final UserRepository userRepository;

    // application.properties の設定値を使用
    @Value("${app.upload-dir}")
    private String uploadDir; // src/main/resources/static/uploads/

    public UserAccountService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ----------------------------------------------------------------------
    // アイコン更新ロジック
    // ----------------------------------------------------------------------

    /**
     * プロフィールアイコンを保存し、公開URLを返す
     * @param file アップロードされた画像ファイル
     * @param userId ユーザーID
     * @return 公開URL (例: /uploads/ユーザーID_xxxxx.png)
     */
    public String saveProfileIcon(MultipartFile file, String userId) throws IOException {
        // 拡張子を取得
        String originalName = file.getOriginalFilename();
        String ext = ".png";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        // ファイル名を一意にする（例: u001_8a2b3c.png）
        String fileName = userId + "_" + UUID.randomUUID() + ext;
        Path filePath = Paths.get(uploadDir, fileName);

        // ディレクトリ作成（存在しない場合）
        Files.createDirectories(filePath.getParent());

        // ファイル保存
        file.transferTo(filePath.toFile());

        // 公開用URL（Spring Bootがstatic配下を公開するため）
        String publicPath = "/uploads/" + fileName;

        // DB更新
        updateUserIcon(userId, publicPath);

        return publicPath;
    }

    /**
     * ユーザーDBのアイコンパスを更新し、更新後のUserを返す
     */
    public User updateUserIcon(String userId, String iconPath) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setIcon(iconPath);
            return userRepository.update(user); // JSON更新
        }
        throw new IllegalArgumentException("User not found for icon update: " + userId);
    }

    // ----------------------------------------------------------------------
    // パスワード更新ロジック
    // ----------------------------------------------------------------------

    /**
     * パスワード更新処理
     */
    public Optional<User> updatePassword(String currentMailAddress, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findByMailAddress(currentMailAddress);

        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();

        // 現在のパスワードを照合
        if (!PasswordUtil.matches(currentPassword, user.getPassword())) {
            return Optional.empty();
        }

        // 同一パスワードを防止
        if (PasswordUtil.isDuplicate(newPassword, user.getPassword())) {
            return Optional.empty();
        }

        // 新パスワードをハッシュ化して更新
        user.setPassword(PasswordUtil.encode(newPassword));
        User updatedUser = userRepository.update(user);

        return Optional.of(updatedUser);
    }

    // ----------------------------------------------------------------------
    // メールアドレス更新ロジック
    // ----------------------------------------------------------------------

    /**
     * メールアドレス更新処理
     */
    public boolean updateEmail(String currentMailAddress, String currentPassword, String newMailAddress) {
        Optional<User> userOpt = userRepository.findByMailAddress(currentMailAddress);

        if (userOpt.isEmpty()) {
            return false;
        }

        // 新しいメールアドレスが既に使用されていないかチェック
        if (userRepository.findByMailAddress(newMailAddress).isPresent()) {
            return false;
        }

        User user = userOpt.get();

        // 現在のパスワード確認
        if (!PasswordUtil.matches(currentPassword, user.getPassword())) {
            return false;
        }

        // メールアドレス更新
        user.setMailAddress(newMailAddress);
        userRepository.update(user);

        return true;
    }
}
