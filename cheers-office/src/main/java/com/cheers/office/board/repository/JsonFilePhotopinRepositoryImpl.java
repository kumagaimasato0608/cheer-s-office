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

import com.cheers.office.board.model.PhotoPin;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Repository
public class JsonFilePhotopinRepositoryImpl implements PhotoPinRepository {

    private final ObjectMapper objectMapper;
    private final File photoPinFile;
    private final CopyOnWriteArrayList<PhotoPin> photoPins;

    public JsonFilePhotopinRepositoryImpl(ObjectMapper objectMapper, @Value("${app.photopin-file-path:src/main/resources/data/photopins.json}") String photoPinFilePath) {
        this.objectMapper = objectMapper.registerModule(new JavaTimeModule());
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
                List<PhotoPin> loadedPins = objectMapper.readValue(photoPinFile, new TypeReference<List<PhotoPin>>() {});
                this.photoPins.clear();
                this.photoPins.addAll(loadedPins);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void savePhotoPinsToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(photoPinFile, photoPins);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<PhotoPin> findAllPins() {
        return new ArrayList<>(photoPins);
    }

    @Override
    public Optional<PhotoPin> findPinById(String pinId) {
        return photoPins.stream()
                .filter(pin -> pin.getPinId().equals(pinId))
                .findFirst();
    }

    @Override
    public PhotoPin savePin(PhotoPin pin) {
        if (pin.getPinId() == null || pin.getPinId().isEmpty()) {
            pin.setPinId(UUID.randomUUID().toString());
        }
        
        Optional<PhotoPin> existingPin = findPinById(pin.getPinId());
        
        if (existingPin.isPresent()) {
            int index = photoPins.indexOf(existingPin.get());
            photoPins.set(index, pin);
        } else {
            photoPins.add(pin);
        }
        
        savePhotoPinsToFile();
        return pin;
    }

    @Override
    public void deletePin(String pinId) {
        photoPins.removeIf(pin -> pin.getPinId().equals(pinId));
        savePhotoPinsToFile();
    }
}