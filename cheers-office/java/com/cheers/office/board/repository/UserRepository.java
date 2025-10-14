// UserRepository.java (最終修正版)

package com.cheers.office.board.repository;

import java.util.List;
import java.util.Optional;

import com.cheers.office.board.model.User;

public interface UserRepository {

    List<User> findAll();
    
    // 実装クラスと名前を一致させる (エラーの原因となっていたfindByUserIdを回避)
    Optional<User> findById(String userId); 
    
    Optional<User> findByMailAddress(String mailAddress);
    
    // ユーザー新規登録時や初期データ保存時に使用 (戻り値はUser)
    User save(User user);
    
    // プロフィール更新用メソッド (戻り値はUser)
    User update(User user); 

    // ユーザー削除用（任意）：deleteByMailAddressは削除します
    void deleteById(String userId);
}