package com.cheers.office.board.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Photo {
    private String photoId;       // 写真の一意識別子
    private String imageUrl;      // 写真のURLまたはファイルパス
    private String comment;       // 写真に関するコメント
    private String uploadedBy;    // アップロードしたユーザーのID
    private String uploadedDate;  // アップロード日時 (ISO 8601形式など)
}