package com.cheers.office.board.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 掲示板データ構造（モデル）
 */
public class ThreadModels {

    /** 共通メッセージ基底クラス */
    public static class MessageBase {
        public String message;
        public String userId;
        public String userName;
        public String icon;
        public boolean anonymous;
        public String timestamp;
    }

    /** リプライ（コメント代替） */
    public static class Reply extends MessageBase {
        public String replyId;
    }

    /** 掲示板スレッド */
    public static class ThreadPost extends MessageBase {
        public String threadId;
        public String title;
        public String imageBase64;
        public List<Reply> replies = new ArrayList<>();
    }
}
