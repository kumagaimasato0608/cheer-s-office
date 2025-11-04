package com.cheers.office.board.repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.Pinit;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * ===============================================================
 * 📍 PinIt機能 - JSONファイル永続化リポジトリ実装クラス
 * ---------------------------------------------------------------
 * ・フォトピンデータを JSON ファイルに保存・読み込みするクラス
 * ・/home/ec2-user/cheers-data/photopins.json に永続化
 * ・Spring Boot 再デプロイやTomcat再起動でもデータ保持
 * ===============================================================
 */
@Repository
public class JsonFilePinitRepositoryImpl implements PinitRepository {

    /** Jackson のシリアライザ／デシリアライザ */
    private final ObjectMapper objectMapper;

    /** 永続化ファイル（フォトピンデータ） */
    private final File pinitFile;

    /** メモリ上のフォトピンリスト（スレッドセーフ） */
    private final CopyOnWriteArrayList<Pinit> pinits;

    /**
     * コンストラクタ：ObjectMapper と 設定ファイルから読み込むパスを初期化
     */
    public JsonFilePinitRepositoryImpl(
            ObjectMapper objectMapper,
            @Value("${app.photopin-file-path:/home/ec2-user/cheers-data/photopins.json}") String photopinFilePath) {

        // ObjectMapperに日付対応モジュールを登録
        this.objectMapper = objectMapper.registerModule(new JavaTimeModule());

        // データファイルを指定
        this.pinitFile = new File(photopinFilePath);

        // メモリ上のリストを初期化
        this.pinits = new CopyOnWriteArrayList<>();

        // ディレクトリが存在しない場合は作成
        File parent = this.pinitFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        // ファイルが存在しない場合は空の JSON 配列で初期化
        if (!this.pinitFile.exists()) {
            try {
                this.pinitFile.createNewFile();
                objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValue(this.pinitFile, new ArrayList<Pinit>());
                System.out.println("[PinIt] photopins.json が存在しなかったため新規作成しました。");
            } catch (IOException e) {
                System.err.println("[PinIt] photopins.json の初期作成に失敗しました。");
                e.printStackTrace();
            }
        }

        // JSONから既存ピンデータをロード
        loadPhotoPins();
    }

    /**
     * 📖 JSONファイルからピンデータを読み込む
     */
    private void loadPhotoPins() {
        try {
            if (pinitFile.exists() && pinitFile.length() > 0) {
                List<Pinit> loadedPins = objectMapper.readValue(
                        pinitFile, new TypeReference<List<Pinit>>() {});
                this.pinits.clear();
                this.pinits.addAll(loadedPins);
                System.out.println("[PinIt] " + pinits.size() + " 件のピンを読み込みました。");
            } else {
                System.out.println("[PinIt] photopins.json は空です。新規状態で開始します。");
            }
        } catch (IOException e) {
            System.err.println("[PinIt] photopins.json の読み込みに失敗しました。");
            e.printStackTrace();
        }
    }

    /**
     * 💾 メモリ上のピンデータを JSON ファイルに保存
     */
    private void savePhotoPinsToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(pinitFile, pinits);
        } catch (IOException e) {
            System.err.println("[PinIt] ピンデータの保存に失敗しました。");
            e.printStackTrace();
        }
    }

    /**
     * 📜 全ピンを取得
     */
    @Override
    public List<Pinit> findAll() {
        return new ArrayList<>(pinits);
    }

    /**
     * 🔍 IDでピンを検索
     */
    @Override
    public Optional<Pinit> findById(String pinId) {
        return pinits.stream()
                     .filter(pin -> pin.getPinId().equals(pinId))
                     .findFirst();
    }

    /**
     * 💾 ピンを新規作成または更新して保存
     */
    @Override
    public Pinit savePin(Pinit pin) {
        if (pin.getPinId() == null || pin.getPinId().isEmpty()) {
            pin.setPinId(UUID.randomUUID().toString());
        }

        Optional<Pinit> existingPin = findById(pin.getPinId());
        if (existingPin.isPresent()) {
            int index = pinits.indexOf(existingPin.get());
            pinits.set(index, pin);
            System.out.println("[PinIt] ピン更新: " + pin.getPinId());
        } else {
            pinits.add(pin);
            System.out.println("[PinIt] 新規ピン追加: " + pin.getPinId());
        }

        savePhotoPinsToFile();
        return pin;
    }

    /**
     * 🗑️ ピンをIDで削除
     */
    @Override
    public void deleteById(String pinId) {
        pinits.removeIf(pin -> pin.getPinId().equals(pinId));
        savePhotoPinsToFile();
        System.out.println("[PinIt] ピン削除: " + pinId);
    }
}
