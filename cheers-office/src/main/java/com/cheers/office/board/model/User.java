package com.cheers.office.board.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class User {
    // JSON定義: user_id
    @JsonProperty("user_id")
    private String userId;

    // JSON定義: user_name
    @JsonProperty("user_name")
    private String userName;

    // JSON定義: mailaddress (ログインID)
    @JsonProperty("mailaddress")
    private String mailAddress;

    // JSON定義: password (ハッシュ化されたパスワードを格納)
    @JsonProperty("password")
    private String password;

    // JSON定義: my_boom
    @JsonProperty("my_boom")
    private String myBoom;

    // JSON定義: group
    @JsonProperty("group")
    private String group;

    // JSON定義: hobby
    @JsonProperty("hobby")
    private String hobby;

    // JSON定義: icon (画像ファイルのローカルパス)
    @JsonProperty("icon")
    private String icon;

    // JSON定義: statusMessage
    @JsonProperty("statusMessage")
    private String statusMessage;
}