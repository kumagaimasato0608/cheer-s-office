package com.cheers.office;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;
import com.cheers.office.util.PasswordUtil;

@SpringBootApplication
public class CheersOfficeApplication extends SpringBootServletInitializer {

    // ✅ Tomcatデプロイ時に必要
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(CheersOfficeApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(CheersOfficeApplication.class, args);
    }

    // アプリ起動時に1回だけ実行される初期化処理
    @Bean
    CommandLineRunner initData(UserRepository userRepository) {
        return args -> {
            userRepository.findByMailAddress("test@cheers.com").ifPresentOrElse(
                user -> {
                    System.out.println("既存のテストユーザーを確認: " + user.getUserName());
                },
                () -> {
                    User testUser = new User();
                    testUser.setUserId("u001_auto");
                    testUser.setUserName("自動登録ユーザー");
                    testUser.setMailAddress("test@cheers.com");
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
