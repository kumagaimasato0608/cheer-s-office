package com.cheers.office.board.model;

import lombok.Data;

@Data
public class Comment {
    private String commentId;
    private String pinId;
    private String userId;
    private String content;
    private String timestamp;
}