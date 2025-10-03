package com.cheers.office.board.model;

import java.io.Serializable; 

public class User implements Serializable { 
    private String userId;
    private String userName;
    private String mailAddress;
    private String password; // ハッシュ化されたパスワード
    private String group;
    private String myBoom;
    private String hobby;
    private String icon;
    private String statusMessage;

    public User() {
    }

    // --- GetterとSetter ---
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getMailAddress() { return mailAddress; }
    public void setMailAddress(String mailAddress) { this.mailAddress = mailAddress; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    public String getMyBoom() { return myBoom; }
    public void setMyBoom(String myBoom) { this.myBoom = myBoom; }
    public String getHobby() { return hobby; }
    public void setHobby(String hobby) { this.hobby = hobby; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
}