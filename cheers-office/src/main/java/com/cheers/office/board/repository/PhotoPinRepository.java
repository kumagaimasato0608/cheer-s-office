package com.cheers.office.board.repository;

import java.util.List;
import java.util.Optional;

import com.cheers.office.board.model.PhotoPin;

public interface PhotoPinRepository {

    // ★★★ このメソッドを追加 ★★★
    List<PhotoPin> findAll();

    Optional<PhotoPin> findById(String pinId);

    PhotoPin savePin(PhotoPin pin);

    void deleteById(String pinId);
}