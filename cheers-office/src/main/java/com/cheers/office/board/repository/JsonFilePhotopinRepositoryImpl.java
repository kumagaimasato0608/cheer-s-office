package com.cheers.office.board.repository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime; // LocalDateTimeを直接扱うように修正
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.PhotoPin;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// PhotopinRepositoryインターフェースの実装クラス
@Repository
public class JsonFilePhotopinRepositoryImpl implements PhotopinRepository {

    private final ObjectMapper objectMapper;
    private final File photoPinFile;
    private final CopyOnWriteArrayList<PhotoPin> photoPins;

    // 💡 プロジェクト構成に合わせて、PhotopinRepositoryインターフェースの定義を以下のように想定します:
    //    PhotoPin savePin(PhotoPin pin);
    //    List<PhotoPin> findAllPins();
    //    Optional<PhotoPin> findPinById(String pinId);
    //    void deletePin(String pinId);

    public JsonFilePhotopinRepositoryImpl(ObjectMapper objectMapper, @Value("${app.photopin-file-path:src/main/resources/data/photopins.json}") String photoPinFilePath) {
        this.objectMapper = objectMapper;
        this.photoPinFile = new File(photoPinFilePath);
        this.photoPins = new CopyOnWriteArrayList<>();
        
        if (!this.photoPinFile.getParentFile().exists()) {
            this.photoPinFile.getParentFile().mkdirs();
        }

        loadPhotoPins();
    }

    private void loadPhotoPins() {
        if (photoPinFile.exists() && photoPinFile.length() > 0) {
            try {
                // ObjectMapperはjackson-datatype-jsr310によってLocalDateTimeを適切に扱います
                List<PhotoPin> loadedPins = objectMapper.readValue(photoPinFile, new TypeReference<List<PhotoPin>>() {});
                this.photoPins.clear();
                this.photoPins.addAll(loadedPins);
                System.out.println("Loaded " + photoPins.size() + " photo pins from " + photoPinFile.getName());
            } catch (IOException e) {
                System.err.println("Failed to load photo pins from file: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Photo pin file does not exist or is empty. Starting with no pins.");
            try {
                photoPinFile.createNewFile();
            } catch (IOException e) {
                System.err.println("Failed to create photo pin file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void savePhotoPins() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(photoPinFile, photoPins);
            System.out.println("Saved " + photoPins.size() + " photo pins to " + photoPinFile.getName());
        } catch (IOException e) {
            System.err.println("Failed to save photo pins to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- PhotopinRepository インターフェース実装 ---

    // 💡 findPinById, findAllPins, savePin, deletePin メソッドを実装します
    
    @Override
    public List<PhotoPin> findAll() {
        return new ArrayList<>(photoPins);
    }

    @Override
    public Optional<PhotoPin> findById(String pinId) {
        return photoPins.stream()
                .filter(pin -> pin.getPinId().equals(pinId))
                .findFirst();
    }

    @Override
    public PhotoPin save(PhotoPin pin) {
        // 💡 savePin に合わせてメソッド名を修正 (save -> savePin)
        if (pin.getPinId() == null || pin.getPinId().isEmpty()) {
            pin.setPinId(UUID.randomUUID().toString()); // 新規ピンにIDを割り当てる
        }
        
        // 💡 日付処理を修正: LocalDateTime型の setCreatedAt() を使用
        if (pin.getCreatedAt() == null) {
             pin.setCreatedAt(LocalDateTime.now()); 
        }

        // 既存のピンを更新するか、新しいピンを追加するか
        boolean updated = false;
        for (int i = 0; i < photoPins.size(); i++) {
            if (photoPins.get(i).getPinId().equals(pin.getPinId())) {
                photoPins.set(i, pin);
                updated = true;
                break;
            }
        }
        if (!updated) {
            photoPins.add(pin);
        }
        savePhotoPins();
        return pin;
    }

    @Override
    public void deleteById(String pinId) {
        // 💡 deletePin に合わせてメソッド名を修正
        photoPins.removeIf(pin -> pin.getPinId().equals(pinId));
        savePhotoPins();
    }
}