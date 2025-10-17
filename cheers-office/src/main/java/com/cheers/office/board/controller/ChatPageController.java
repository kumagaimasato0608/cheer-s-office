package com.cheers.office.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatPageController {

    /** チャット画面（chat.html）を表示 */
    @GetMapping("/chat/rooms")
    public String showChatPage() {
        return "chat"; // templates/chat.html を表示
    }
}
