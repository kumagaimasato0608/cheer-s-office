package com.cheers.office.board.repository;

import java.util.List;
import java.util.Optional;

import com.cheers.office.board.model.Pinit;

public interface PinitRepository {

    // ★★★ このメソッドを追加 ★★★
    List<Pinit> findAll();

    Optional<Pinit> findById(String pinId);

    Pinit savePin(Pinit pin);

    void deleteById(String pinId);
}