package com.cheers.office.board.repository;

import java.util.List;
import java.util.Optional;

import com.cheers.office.board.model.User;

// JpaRepositoryの使用をやめ、シンプルなインターフェースに修正 (以前確認済み)
public interface UserRepository {

    List<User> findAll();

    Optional<User> findByMailAddress(String mailAddress); // ログインで使用

    Optional<User> findById(String userId);

    User save(User user);
}