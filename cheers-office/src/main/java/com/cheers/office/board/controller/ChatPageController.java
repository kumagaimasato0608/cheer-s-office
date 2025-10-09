package com.cheers.office.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatPageController {

    @GetMapping("/chat")
    public String chatPage(Model model) {
        return "chat"; // → templates/chat.html
    }

    @GetMapping("/chat/rooms")
    public String chatRoomsPage(Model model) {
        return "chat";
    }
}
