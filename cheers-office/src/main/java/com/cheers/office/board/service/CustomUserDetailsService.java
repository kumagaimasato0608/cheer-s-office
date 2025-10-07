package com.cheers.office.board.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Spring Securityが認証時にユーザー名を元にユーザー情報を取得するために呼び出すメソッド。
     * ここでの「username」は、ログインフォームで入力されたメールアドレスを指します。
     */
    @Override
    public UserDetails loadUserByUsername(String mailAddress) throws UsernameNotFoundException {
        
        // ★★★ ここを修正 ★★★
        // findByMailAddressで見つかったUserオブジェクトをCustomUserDetailsでラップして返す。
        // もしユーザーが見つからない場合は、nullを返すのではなく、必ずUsernameNotFoundExceptionをスローする。
        
        User user = userRepository.findByMailAddress(mailAddress)
                .orElseThrow(() -> new UsernameNotFoundException("指定されたメールアドレスのユーザーが見つかりません: " + mailAddress));

        return new CustomUserDetails(user);
    }
}