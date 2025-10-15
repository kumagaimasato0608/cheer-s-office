package com.cheers.office;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;
import com.cheers.office.util.PasswordUtil;

@SpringBootApplication
public class CheersOfficeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CheersOfficeApplication.class, args);
	}

    // アプリケーション起動時に一度だけ実行される初期化メソッド
    @Bean
    CommandLineRunner initData(UserRepository userRepository) {
        return args -> {
            // ★★★ テストユーザーの存在チェックと自動登録 ★★★
            userRepository.findByMailAddress("test@cheers.com").ifPresentOrElse(
                user -> {
                    // ユーザーが既に存在する場合 (何もしないか、ログ出力)
                    System.out.println("既存のテストユーザーを確認: " + user.getUserName());
                },
                () -> {
                    // ユーザーが存在しない場合、新規作成
                    User testUser = new User();
                    testUser.setUserId("u001_auto"); // 新しい一意なID
                    testUser.setUserName("自動登録ユーザー");
                    testUser.setMailAddress("test@cheers.com");
                    
                    // PasswordUtil を使って「password」をハッシュ化し設定
                    testUser.setPassword(PasswordUtil.encode("password")); 
                    
                    testUser.setMyBoom("Java開発");
                    testUser.setGroup("開発部");
                    testUser.setHobby("読書");
                    testUser.setIcon("/images/default_icon.png");
                    testUser.setStatusMessage("よろしくお願いします！");
                    
                    userRepository.save(testUser);
                    System.out.println("★新規テストユーザーを 'password' で自動登録しました。");
                }
            );
        };
    }
}