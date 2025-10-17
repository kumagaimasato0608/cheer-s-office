package com.cheers.office.board.controller;

import java.util.List;
import java.util.Optional; // この行がインポートされていることを確認
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // この行がインポートされていることを確認
import org.springframework.web.bind.annotation.RestController;

import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;

@RestController
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/users/me")
    public ResponseEntity<User> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            User originalUser = userDetails.getUser();
            
            User userForResponse = new User();
            userForResponse.setUserId(originalUser.getUserId());
            userForResponse.setUserName(originalUser.getUserName());
            userForResponse.setMailAddress(originalUser.getMailAddress());
            userForResponse.setIcon(originalUser.getIcon());
            userForResponse.setGroup(originalUser.getGroup());
            userForResponse.setMyBoom(originalUser.getMyBoom());
            userForResponse.setHobby(originalUser.getHobby());
            userForResponse.setStatusMessage(originalUser.getStatusMessage());
            userForResponse.setTeamColor(originalUser.getTeamColor());
            
            return ResponseEntity.ok(userForResponse);
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/api/users")
    public List<User> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> {
                    User safeUser = new User();
                    safeUser.setUserId(user.getUserId());
                    safeUser.setUserName(user.getUserName());
                    safeUser.setIcon(user.getIcon());
                    safeUser.setTeamColor(user.getTeamColor());
                    return safeUser;
                })
                .collect(Collectors.toList());
    }

    // ▼▼▼ このメソッドをファイルの一番下に追加してください ▼▼▼
    /**
     * 指定されたIDのユーザー詳細情報を取得するAPI
     * チャット画面が呼び出すのはこのエンドポイントです。
     * @param userId 検索するユーザーのID
     * @return ユーザーの完全なプロフィール詳細、または404 Not Foundエラー
     */
    @GetMapping("/api/users/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        // リポジトリを使い、IDでユーザーを検索します
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isPresent()) {
            User originalUser = userOptional.get();

            // パスワードなどの機密情報を除いた、安全なUserオブジェクトを作成して返します
            User safeUser = new User();
            safeUser.setUserId(originalUser.getUserId());
            safeUser.setUserName(originalUser.getUserName());
            safeUser.setMailAddress(originalUser.getMailAddress());
            safeUser.setIcon(originalUser.getIcon());
            safeUser.setGroup(originalUser.getGroup());
            safeUser.setMyBoom(originalUser.getMyBoom());
            safeUser.setHobby(originalUser.getHobby());
            safeUser.setStatusMessage(originalUser.getStatusMessage());
            safeUser.setTeamColor(originalUser.getTeamColor());
            
            // ユーザーが見つかった場合、そのデータを200 OKステータスで返します
            return ResponseEntity.ok(safeUser);
        } else {
            // ユーザーが見つからなかった場合、404 Not Foundステータスを返します
            return ResponseEntity.notFound().build();
        }
    }
}