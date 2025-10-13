package com.cheers.office.board.model;

// シンプルなデータクラスとしてrecordを使用します
public record BonusZone(String name, double latitude, double longitude, int radius, int points) {
}