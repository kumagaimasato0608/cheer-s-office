package com.cheers.office.board.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;
import com.cheers.office.util.PasswordUtil;

@Service
public class UserAccountService {

    private final UserRepository userRepository;
    
    // 【重要】ファイルの保存先ディレクトリを定義
    // アプリケーションが読み込める静的リソースの場所に設定
    private static final String ICON_UPLOAD_DIR = "src/main/resources/static/images/profile/";

    public UserAccountService(UserRepository userRepository) {
        this.userRepository = userRepository;
        // ディレクトリが存在しない場合は作成
        new File(ICON_UPLOAD_DIR).mkdirs();
    }

    // ----------------------------------------------------------------------
    // アイコン更新ロジック
    // ----------------------------------------------------------------------

    /**
     * プロフィールアイコンをディスクに保存し、ユーザーDBのアイコンパスを更新する
     * @param file アップロードされた切り抜き済みファイル
     * @param userId ユーザーID
     * @return 保存されたアイコンの公開パス (例: /images/profile/ユーザーID.png)
     */
    public String saveProfileIcon(MultipartFile file, String userId) throws IOException {
        
        // ファイル名: ユーザーID.png (既存ファイルを上書きすることで一意性を担保)
        String fileName = userId + ".png";
        Path filePath = Paths.get(ICON_UPLOAD_DIR, fileName);
        
        // 1. ファイルをディスクに書き込む
        Files.write(filePath, file.getBytes());

        // 2. ユーザーのDB情報を更新
        String publicPath = "/images/profile/" + fileName;
        updateUserIcon(userId, publicPath); // DB更新
        
        return publicPath; // 新しいアイコンパスをControllerに返す
    }
    
    /**
     * ユーザーDBのアイコンパスを更新し、更新されたUserオブジェクトを返す
     */
    public User updateUserIcon(String userId, String iconPath) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // パスワードなどの既存情報を保持したまま、アイコンパスのみを変更
            user.setIcon(iconPath);
            
            // UserRepositoryのupdateメソッドで保存し、更新されたユーザーを返す
            return userRepository.update(user);
        }
        
        throw new IllegalArgumentException("User not found for icon update: " + userId);
    }
    
    // ----------------------------------------------------------------------
    // アカウント更新ロジック (MyPageControllerで使用)
    // ----------------------------------------------------------------------

    /**
     * パスワード更新処理を実行
     * @param currentMailAddress ログイン中のユーザーのメールアドレス
     * @param currentPassword ユーザーが入力した現在のパスワード(平文)
     * @param newPassword ユーザーが入力した新しいパスワード(平文)
     * @return 更新後のUserオブジェクト (失敗時はOptional.empty())
     */
    public Optional<User> updatePassword(String currentMailAddress, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findByMailAddress(currentMailAddress);

        if (userOpt.isEmpty()) {
            return Optional.empty(); 
        }

        User user = userOpt.get();

        // 1. 現在のパスワードの検証
        if (!PasswordUtil.matches(currentPassword, user.getPassword())) {
            return Optional.empty(); // パスワード不一致
        }

        // 2. 新しいパスワードが現在のハッシュ値と同じでないかチェック
        if (PasswordUtil.isDuplicate(newPassword, user.getPassword())) {
             return Optional.empty(); // 新しいパスワードが現在のパスワードと同じ
        }
        
        // 3. パスワードをハッシュ化して更新
        user.setPassword(PasswordUtil.encode(newPassword));
        
        // 4. リポジトリに保存
        User updatedUser = userRepository.update(user);
        
        return Optional.of(updatedUser);
    }
    
    /**
     * メールアドレス更新処理を実行
     */
    public boolean updateEmail(String currentMailAddress, String currentPassword, String newMailAddress) {
        Optional<User> userOpt = userRepository.findByMailAddress(currentMailAddress);

        if (userOpt.isEmpty()) {
            return false; // ユーザーが存在しない
        }
        
        // 新しいメールアドレスが既に他のユーザーに使用されていないかチェック
        if (userRepository.findByMailAddress(newMailAddress).isPresent()) {
            return false; // すでに使用されている
        }

        User user = userOpt.get();

        // 1. 現在のパスワードの検証
        if (!PasswordUtil.matches(currentPassword, user.getPassword())) {
            return false; // パスワード不一致
        }
        
        // 2. メールアドレスを更新
        user.setMailAddress(newMailAddress);
        
        // 3. リポジトリに保存
        userRepository.update(user);
        
        return true;
    }
}