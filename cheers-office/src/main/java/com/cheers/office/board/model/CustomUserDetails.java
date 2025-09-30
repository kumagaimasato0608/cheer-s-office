package com.cheers.office.board.model; // ★このパッケージ宣言が正しいことを確認

import java.util.Collection;
import java.util.Collections; // Collections は必要だが、今回はSimpleGrantedAuthorityで直接ロールを返します

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority; // このimportを追加
import org.springframework.security.core.userdetails.UserDetails;

// UserDetails インターフェースを実装することで、Spring Securityがユーザーを認証・認可する際に使用します
public class CustomUserDetails implements UserDetails {

    private final User user; // 実際のUserモデルへの参照

    public CustomUserDetails(User user) {
        this.user = user;
    }

    // UserDetailsが提供するメソッドの実装
    // --- 権限関連 ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 例: 全てのユーザーに "ROLE_USER" の権限を与える
        // 必要に応じて、Userモデルにロール情報を持たせ、それをここから返します
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    // --- ユーザー認証情報関連 ---
    @Override
    public String getPassword() {
        return user.getPassword(); // Userモデルからパスワードを返す
    }

    @Override
    public String getUsername() {
        return user.getMailAddress(); // ユーザー名としてメールアドレスを使用（またはuserIdなど）
    }

    // --- アカウントの状態関連 ---
    @Override
    public boolean isAccountNonExpired() {
        return true; // アカウントの有効期限が切れていないか
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // アカウントがロックされていないか
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 資格情報（パスワード）の有効期限が切れていないか
    }

    @Override
    public boolean isEnabled() {
        return true; // アカウントが有効か
    }

    // --- その他、必要に応じてUserモデルの情報を取得するメソッド ---
    public User getUser() {
        return user;
    }

    public String getDisplayName() {
        return user.getUserName();
    }
}