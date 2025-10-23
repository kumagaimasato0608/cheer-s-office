package com.cheers.office.board.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class User implements Serializable { 
    private String userId;
    private String userName;
    private String mailAddress;
    private String password;
    private String group;
    private String myBoom;
    private String hobby;
    private String icon;
    private String statusMessage;
    private String teamColor;

    // ★★★ 新規追加フィールド ★★★
    private String deploymentDestination;
    private String deploymentArea;
    private String commuteFrequency;
    private String workTime;
    private String workContent;
    
    // ★★★ 修正箇所: チュートリアル表示完了フラグを追加 ★★★
    private boolean tutorialSeen; 
    
    // ★★★ 新規追加フィールド ここまで ★★★

    // ★★★ 最後にピンを置いた日時を記録するフィールドを追加 ★★★
    private LocalDateTime lastPinTimestamp;
    
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
    public String getTeamColor() { return teamColor; }
    public void setTeamColor(String teamColor) { this.teamColor = teamColor; }

    // ★★★ 新規追加フィールド用のGetterとSetter ★★★
    public String getDeploymentDestination() { return deploymentDestination; }
    public void setDeploymentDestination(String deploymentDestination) { this.deploymentDestination = deploymentDestination; }
    public String getDeploymentArea() { return deploymentArea; }
    public void setDeploymentArea(String deploymentArea) { this.deploymentArea = deploymentArea; }
    public String getCommuteFrequency() { return commuteFrequency; }
    public void setCommuteFrequency(String commuteFrequency) { this.commuteFrequency = commuteFrequency; }
    public String getWorkTime() { return workTime; }
    public void setWorkTime(String workTime) { this.workTime = workTime; }
    public String getWorkContent() { return workContent; }
    public void setWorkContent(String workContent) { this.workContent = workContent; }
    
    // ★★★ チュートリアル完了フラグ用のGetterとSetterを追加 ★★★
    public boolean isTutorialSeen() { return tutorialSeen; }
    public void setTutorialSeen(boolean tutorialSeen) { this.tutorialSeen = tutorialSeen; }
    
    // ★★★ lastPinTimestamp用のGetterとSetterを追加 ★★★
    public LocalDateTime getLastPinTimestamp() { return lastPinTimestamp; }
    public void setLastPinTimestamp(LocalDateTime lastPinTimestamp) { this.lastPinTimestamp = lastPinTimestamp; }
}