package com.cheers.office.board.repository;

import java.util.List;
import java.util.Optional;

import com.cheers.office.board.model.PhotoPin;

/**
 * フォトピンのデータアクセス操作を定義するインターフェース。
 * JsonFilePhotopinRepositoryImplはこのインターフェースを実装します。
 */
public interface PhotopinRepository {

    /**
     * すべてのPhotoPinを取得する
     * @return PhotoPinのリスト
     */
    List<PhotoPin> findAll();

    /**
     * 指定されたIDのPhotoPinを検索する
     * @param pinId PhotoPinのID (String)
     * @return 存在する場合はPhotoPin、存在しない場合はOptional.empty()
     */
    Optional<PhotoPin> findById(String pinId);

    /**
     * PhotoPinを保存または更新する
     * (新規登録の場合はIDを付与し、既存の場合は上書き)
     * @param photoPin 保存するPhotoPin
     * @return 保存されたPhotoPin
     */
    PhotoPin save(PhotoPin photoPin);

    /**
     * 指定されたIDのPhotoPinを削除する
     * @param pinId 削除するPhotoPinのID (String)
     */
    void deleteById(String pinId);
}