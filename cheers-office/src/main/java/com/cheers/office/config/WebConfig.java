package com.cheers.office.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // === 各アップロード先パスを読み込み ===
    @Value("${app.upload-dir.chat}")
    private String chatUploadDir;

    @Value("${app.upload-dir.profile}")
    private String profileUploadDir;

    @Value("${app.upload-dir.group}")
    private String groupUploadDir;

    @Value("${app.upload-dir.thread}")
    private String threadUploadDir;

    @Value("${app.upload-dir.photopin}")
    private String photopinUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // --- チャット画像 ---
        registry.addResourceHandler("/images/chat/**")
                .addResourceLocations("file:" + chatUploadDir + "/");

        // --- プロフィール画像 ---
        registry.addResourceHandler("/images/profile/**")
                .addResourceLocations("file:" + profileUploadDir + "/");

        // --- グループアイコン ---
        registry.addResourceHandler("/images/groups/**")
                .addResourceLocations("file:" + groupUploadDir + "/");

        // ✅ 掲示板画像（thread）---
        registry.addResourceHandler("/images/thread/**")
                .addResourceLocations("file:" + threadUploadDir + "/");

        // ✅ フォトピン画像（photopins）---
        registry.addResourceHandler("/images/photopins/**")
                .addResourceLocations("file:" + photopinUploadDir + "/");
    }
}
