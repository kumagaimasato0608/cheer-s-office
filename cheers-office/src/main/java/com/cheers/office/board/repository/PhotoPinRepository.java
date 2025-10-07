package com.cheers.office.board.repository;

import java.util.List;
import java.util.Optional;

import com.cheers.office.board.model.PhotoPin;

public interface PhotoPinRepository {

    List<PhotoPin> findAllPins();

    Optional<PhotoPin> findPinById(String pinId);

    PhotoPin savePin(PhotoPin pin);

    void deletePin(String pinId);
}