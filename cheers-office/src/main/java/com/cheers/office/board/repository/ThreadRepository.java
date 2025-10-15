package com.cheers.office.board.repository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.cheers.office.board.model.ThreadModels.ThreadPost;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 掲示板データをJSONで保存／読み込みするリポジトリ
 */
@Repository
public class ThreadRepository {

    private static final String FILE_PATH = "src/main/resources/data/thread.json";
    private final ObjectMapper mapper = new ObjectMapper();

    /** 全件取得（ファイルがなければ空リスト） */
    public synchronized List<ThreadPost> findAll() {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return new ArrayList<>();
            return mapper.readValue(file, new TypeReference<List<ThreadPost>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /** 保存（全件上書き） */
    public synchronized void saveAll(List<ThreadPost> posts) {
        try {
            File file = new File(FILE_PATH);
            file.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, posts);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
