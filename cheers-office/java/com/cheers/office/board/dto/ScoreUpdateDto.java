package com.cheers.office.board.dto;

// record を使うと、フィールドを定義するだけで自動的に
// コンストラクタやGetterが作られるため便利です。
public record ScoreUpdateDto(int red, int blue, int yellow) {
}