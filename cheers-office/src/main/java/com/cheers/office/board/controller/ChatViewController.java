package com.cheers.office.board.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cheers.office.board.model.Room;
import com.cheers.office.board.repository.RoomRepository;

@Controller
@RequestMapping("/chat")
public class ChatViewController {

    private final RoomRepository roomRepository;

    public ChatViewController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /**
     * チャットルーム一覧を表示
     */
    @GetMapping("/rooms")
    public String showRoomList(Model model) {
        // 実際にはログインユーザーが参加しているルームのみを抽出する
        List<Room> rooms = roomRepository.findAll(); 
        model.addAttribute("rooms", rooms);
        return "chat_rooms"; // src/main/resources/templates/chat_rooms.html を参照
    }

    /**
     * 特定のチャットルームの画面を表示
     */
    @GetMapping("/{roomId}")
    public String showChatRoom(@PathVariable String roomId, Model model) {
        // 選択されたルームIDを画面に渡す
        model.addAttribute("roomId", roomId);
        return "chat_room"; // src/main/resources/templates/chat_room.html を参照
    }
}