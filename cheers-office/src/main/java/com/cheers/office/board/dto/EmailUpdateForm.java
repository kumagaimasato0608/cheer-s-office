package com.cheers.office.board.dto;

/**
 * メールアドレス変更フォームからのデータを受け取るDTO
 */
public class EmailUpdateForm {

    // HTMLのフォームフィールド名と一致させる
    private String currentMailAddress; 
    private String newMailAddress;
    private String currentPasswordForEmail; 

    // Springがデータをバインドするために必須
    public String getCurrentMailAddress() { return currentMailAddress; }
    public void setCurrentMailAddress(String currentMailAddress) { this.currentMailAddress = currentMailAddress; }

    public String getNewMailAddress() { return newMailAddress; }
    public void setNewMailAddress(String newMailAddress) { this.newMailAddress = newMailAddress; }

    public String getCurrentPasswordForEmail() { return currentPasswordForEmail; }
    public void setCurrentPasswordForEmail(String currentPasswordForEmail) { this.currentPasswordForEmail = currentPasswordForEmail; }
}