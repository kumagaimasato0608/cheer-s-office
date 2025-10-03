package com.cheers.office.board.repository;

import java.util.List;
import java.util.Optional;

import com.cheers.office.board.model.User;

public interface UserRepository {
    Optional<User> findByMailAddress(String mailAddress);
    Optional<User> findByUserId(String userId); // findByUserId を明示的に定義
    User save(User user);
    User update(User user);
    
    void deleteByMailAddress(String mailAddress); // 追加
    List<User> findAll(); // 追加
    void deleteById(String userId); // 追加
    // Optional<User> findById(String userId); // findByUserId と同じなので、今回はどちらか一方に統一します
}