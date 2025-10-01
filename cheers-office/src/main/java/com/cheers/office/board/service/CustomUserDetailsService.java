package com.cheers.office.board.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.cheers.office.board.model.CustomUserDetails; // CustomUserDetailsのインポート
import com.cheers.office.board.repository.UserRepository; // ★UserRepositoryのインポート

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // UserRepositoryを注入
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * ログイン画面で入力されたメールアドレス(username)を使ってユーザーを検索する
     */
    @Override
    public UserDetails loadUserByUsername(String mailAddress) throws UsernameNotFoundException {
        
        // UserRepositoryを使ってメールアドレスに一致するユーザーをJSON DBから検索
        return userRepository.findByMailAddress(mailAddress)
                .map(CustomUserDetails::new) // ユーザーが見つかったらCustomUserDetailsに変換
                .orElseThrow(() -> 
                        // 見つからなかった場合は例外を投げる (Spring Securityが必要とする処理)
                        new UsernameNotFoundException("User not found with mailaddress: " + mailAddress));
    }
}