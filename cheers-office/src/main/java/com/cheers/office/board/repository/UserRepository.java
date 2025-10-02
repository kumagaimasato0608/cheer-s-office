package com.cheers.office.board.repository;

import java.util.List;
import java.util.Optional;

import com.cheers.office.board.model.User;

// ★JpaRepository の継承は使用しません
public interface UserRepository {

    List<User> findAll();
    
    Optional<User> findById(String userId);
    
    Optional<User> findByMailAddress(String mailAddress);
    
    // ユーザー新規登録時や初期データ保存時に使用
    User save(User user);
    
    // ★★★ プロフィール更新用メソッド (新規追加) ★★★
    User update(User user); 

    // ユーザー削除用（任意）
    void deleteById(String userId);
}