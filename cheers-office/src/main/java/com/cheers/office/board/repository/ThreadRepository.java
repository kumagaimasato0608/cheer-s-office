package com.cheers.office.board.repository;

import java.io.IOException; // ✅ IOException をインポート
import java.nio.charset.StandardCharsets; // ✅ 文字コード指定のためインポート
import java.nio.file.Files; // ✅ Files クラスをインポート
import java.nio.file.Path; // ✅ Path をインポート
import java.nio.file.Paths; // ✅ Paths をインポート
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger; // ✅ Logger をインポート
import org.slf4j.LoggerFactory; // ✅ LoggerFactory をインポート
import org.springframework.beans.factory.annotation.Value; // ✅ @Value をインポート
import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.ThreadModels.ThreadPost;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 掲示板データをJSONで保存／読み込みするリポジトリ
 */
@Repository
public class ThreadRepository {

    // ✅ Logger を追加 (エラーハンドリング改善のため)
    private static final Logger log = LoggerFactory.getLogger(ThreadRepository.class);

    // 🛑 修正: ハードコードされたパスを削除
    // private static final String FILE_PATH = "src/main/resources/data/thread.json";

    // ✅ 修正: application.properties からファイルパスをインジェクション
    private final Path filePath;
    private final ObjectMapper mapper; // ObjectMapper もインジェクションで受け取るのが一般的

    // ✅ 修正: コンストラクタで ObjectMapper と @Value を受け取る
    public ThreadRepository(ObjectMapper objectMapper,
                           // application.properties の app.thread-file-path の値がインジェクトされる
                           @Value("${app.thread-file-path}") String threadFilePath) {
        this.mapper = objectMapper;
        // application.properties で指定されたパス文字列から Path オブジェクトを作成
        this.filePath = Paths.get(threadFilePath);

        // 起動時に親ディレクトリが存在するか確認し、なければ作成する
        ensureParentDirectoryExists(this.filePath);
    }

    /** 全件取得（ファイルがなければ空リスト） */
    public synchronized List<ThreadPost> findAll() {
        // ✅ 修正: Path を使用し、文字コードを指定
        if (Files.notExists(filePath) || !Files.isReadable(filePath)) {
            // ⚠️ WARN: 指定されたパスにファイルが見つからない、または読めない
            log.warn("掲示板データファイルが見つからないか、読み取れません: {}", filePath);
            return new ArrayList<>();
        }
        try {
            if (Files.size(filePath) == 0) {
                return new ArrayList<>(); // 空ファイルの場合は空リスト
            }
            // 文字コード UTF-8 を指定して読み込む
            return mapper.readValue(Files.newBufferedReader(filePath, StandardCharsets.UTF_8),
                                    new TypeReference<List<ThreadPost>>() {});
        } catch (IOException e) { // ✅ 修正: IOException をキャッチ
            log.error("掲示板データファイルの読み込みに失敗しました: {}", filePath, e);
            return new ArrayList<>(); // エラー時は空リスト
        }
    }

    /** 保存（全件上書き） */
    public synchronized void saveAll(List<ThreadPost> posts) {
        try {
            // ✅ 修正: Path を使用し、文字コードを指定
            // 親ディレクトリの存在はコンストラクタで確認済み
            // 文字コード UTF-8 を指定して書き込む
            mapper.writerWithDefaultPrettyPrinter().writeValue(
                Files.newBufferedWriter(filePath, StandardCharsets.UTF_8),
                posts);
        } catch (IOException e) { // ✅ 修正: IOException をキャッチ
            log.error("掲示板データファイルへの書き込みに失敗しました: {}", filePath, e);
            // 本番環境ではエラーをログに記録するだけでなく、
            // 呼び出し元に例外をスローするなど、より適切なエラー処理が必要な場合があります。
            // throw new RuntimeException("Failed to save thread data to " + filePath, e);
        }
    }

    // ✅ 追加: 親ディレクトリを作成するユーティリティメソッド
    private void ensureParentDirectoryExists(Path path) {
        Path parent = path.getParent();
        if (parent != null && Files.notExists(parent)) {
            try {
                Files.createDirectories(parent);
                log.info("データディレクトリを作成しました: {}", parent);
            } catch (IOException e) {
                log.error("データディレクトリの作成に失敗しました: {}", parent, e);
                // 起動時の致命的なエラーとして処理を中断させることも検討できます
                throw new RuntimeException("Failed to create data directory: " + parent, e);
            }
        }
    }
}
