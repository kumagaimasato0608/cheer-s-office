package com.cheers.office.board.dto;

/**
 * パスワード変更フォームからのデータを受け取るDTO
 */
public class PasswordUpdateForm {

    // HTMLのフォームフィールド名と一致させる
    private String currentMailAddress; 
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    // Springがデータをバインドするために必須
    public String getCurrentMailAddress() { return currentMailAddress; }
    public void setCurrentMailAddress(String currentMailAddress) { this.currentMailAddress = currentMailAddress; }

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}