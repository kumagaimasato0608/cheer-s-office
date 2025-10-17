package com.cheers.office.board.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChatService {

    /**
     * 画像を保存し、Webからアクセス可能なパスを返す（汎用版）
     * @param file アップロードされたファイル
     * @param uploadDir 保存先の物理ディレクトリパス
     * @param urlPrefix フロントエンドで使うURLの接頭辞 (例: "/images/groups")
     * @return Webからアクセス可能な画像のフルパス
     * @throws IOException ファイルの保存に失敗した場合
     */
    public String saveImage(MultipartFile file, String uploadDir, String urlPrefix) throws IOException {
        // 1. 安全な方法で保存先パスを解決
        Path uploadPath = Paths.get(uploadDir);

        // 2. フォルダがなければ作成
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 3. ファイル名が一意になるように生成
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String newFileName = UUID.randomUUID().toString() + "." + fileExtension;

        // 4. ファイルを実際に保存
        Path filePath = uploadPath.resolve(newFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 5. 正しいURLを組み立てて返す
        return urlPrefix + "/" + newFileName;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "png"; // 拡張子がない場合はデフォルトでpngにする
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}