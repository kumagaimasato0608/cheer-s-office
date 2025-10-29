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

@Repository
public class JsonFilePinitRepositoryImpl implements PinitRepository {

    private final ObjectMapper objectMapper;
    private final File PinitFile;
    private final CopyOnWriteArrayList<Pinit> Pinits;

    public JsonFilePinitRepositoryImpl(ObjectMapper objectMapper, @Value("${app.Pinit-file-path:src/main/resources/data/photopins.json}") String PinitFilePath) {
        this.objectMapper = objectMapper.registerModule(new JavaTimeModule());
        this.PinitFile = new File(PinitFilePath);
        this.Pinits = new CopyOnWriteArrayList<>();
        
        if (!this.PinitFile.getParentFile().exists()) {
            this.PinitFile.getParentFile().mkdirs();
        }
        loadPhotoPins();
    }

    private void loadPhotoPins() {
        if (PinitFile.exists() && PinitFile.length() > 0) {
            try {
                List<Pinit> loadedPins = objectMapper.readValue(PinitFile, new TypeReference<List<Pinit>>() {});
                this.Pinits.clear();
                this.Pinits.addAll(loadedPins);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void savePhotoPinsToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(PinitFile, Pinits);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    // ★★★ メソッド名を findAllPins から findAll に修正 ★★★
    public List<Pinit> findAll() {
        return new ArrayList<>(Pinits);
    }

    @Override
    // ★★★ メソッド名を findPinById から findById に修正 ★★★
    public Optional<Pinit> findById(String pinId) {
        return Pinits.stream()
                .filter(pin -> pin.getPinId().equals(pinId))
                .findFirst();
    }

    @Override
    public Pinit savePin(Pinit pin) {
        if (pin.getPinId() == null || pin.getPinId().isEmpty()) {
            pin.setPinId(UUID.randomUUID().toString());
        }
        
        // findById に修正したメソッドを呼び出す
        Optional<Pinit> existingPin = findById(pin.getPinId());
        
        if (existingPin.isPresent()) {
            int index = Pinits.indexOf(existingPin.get());
            Pinits.set(index, pin);
        } else {
            Pinits.add(pin);
        }
        
        savePhotoPinsToFile();
        return pin;
    }

    @Override
    // ★★★ メソッド名を deletePin から deleteById に修正 ★★★
    public void deleteById(String pinId) {
        Pinits.removeIf(pin -> pin.getPinId().equals(pinId));
        savePhotoPinsToFile();
    }
}