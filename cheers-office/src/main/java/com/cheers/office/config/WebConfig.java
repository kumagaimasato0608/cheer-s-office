package com.cheers.office.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // application.propertiesからチャット画像の保存パスを取得
    @Value("${app.upload-dir.chat}")
    private String chatUploadDir;

    // application.propertiesからプロフィール画像の保存パスを取得
    @Value("${app.upload-dir.profile}")
    private String profileUploadDir;
    
    // application.propertiesからグループアイコンの保存パスを取得
    @Value("${app.upload-dir.group}")
    private String groupUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        
        // --- チャット画像のパス設定 ---
        // URLパス "/images/chat/**" へのアクセスを、
        // 物理フォルダ "src/main/resources/static/images/chat/" などに関連付ける
        registry.addResourceHandler("/images/chat/**")
                .addResourceLocations("file:" + chatUploadDir + "/");

        // --- プロフィール画像のパス設定 ---
        registry.addResourceHandler("/images/profile/**")
                .addResourceLocations("file:" + profileUploadDir + "/");
                
        // ✅ グループアイコンのパス設定 (これが最も重要！) ---
        // URLパス "/images/groups/**" へのアクセスを、
        // 物理フォルダ "src/main/resources/static/images/groups/" などに関連付ける
        registry.addResourceHandler("/images/groups/**")
                .addResourceLocations("file:" + groupUploadDir + "/");
    }
}