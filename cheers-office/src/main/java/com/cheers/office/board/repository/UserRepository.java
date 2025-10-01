package com.cheers.office.board.repository;

import java.util.List;
import java.util.Optional;

import com.cheers.office.board.model.User;
// import java.util.UUID; // UUIDは使わないので削除

// JpaRepository を継承しない
public interface UserRepository {
    List<User> findAll();
    Optional<User> findById(String userId); // UUIDではなくString
    Optional<User> findByMailAddress(String mailAddress);
    User save(User user);
    void deleteById(String userId); // 必要であれば追加
}