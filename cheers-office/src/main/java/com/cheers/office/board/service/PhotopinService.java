package com.cheers.office.board.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cheers.office.board.dto.PhotoUploadForm;
import com.cheers.office.board.model.PhotoPin;
import com.cheers.office.board.repository.PhotopinRepository;

@Service
public class PhotopinService {

    private final PhotopinRepository photopinRepository;
    
    // 【重要】写真ファイルの保存先ディレクトリを定義
    // static/images/photopins/ に保存されることを想定
    private static final String PHOTO_UPLOAD_DIR = "src/main/resources/static/images/photopins/";

    public PhotopinService(PhotopinRepository photopinRepository) {
        this.photopinRepository = photopinRepository;
        // ディレクトリが存在しない場合は作成
        new File(PHOTO_UPLOAD_DIR).mkdirs();
    }

    /**
     * 新しいフォトピンを保存する（ファイルI/OとDB登録の両方を行う）
     * @param form アップロードフォームデータ
     * @param userId 投稿ユーザーID
     * @return 登録されたPhotoPinオブジェクト
     * @throws IOException ファイル保存失敗時
     */
    public PhotoPin savePhotoPin(PhotoUploadForm form, String userId) throws IOException {
        
        // 1. ファイルをディスクに保存
        MultipartFile file = form.getFile();
        String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(PHOTO_UPLOAD_DIR, uniqueFileName);
        
        // ファイルをサーバーのディスクに書き込む
        Files.write(filePath, file.getBytes());

        // 2. DBモデルを作成し、永続化
        String publicPath = "/images/photopins/" + uniqueFileName;
        
        PhotoPin newPin = new PhotoPin();
        newPin.setPinId(UUID.randomUUID().toString());
        newPin.setUserId(userId);
        newPin.setPhotoPath(publicPath); // 公開パスを設定
        newPin.setCaption(form.getCaption());
        newPin.setLatitude(form.getLatitude());
        newPin.setLongitude(form.getLongitude());
        newPin.setCreatedAt(LocalDateTime.now());
        
        // 3. Repositoryに登録
        return photopinRepository.save(newPin);
    }
    
    /**
     * 地図表示のためにすべてのフォトピンデータを取得する
     */
    public List<PhotoPin> findAllPhotoPins() {
        return photopinRepository.findAll();
    }
}