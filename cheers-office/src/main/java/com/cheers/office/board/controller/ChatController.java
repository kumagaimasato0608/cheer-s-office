package com.cheers.office.board.controller;

import java.time.LocalDateTime;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import com.cheers.office.board.model.ChatMessage;
import com.cheers.office.board.model.Log;
import com.cheers.office.board.repository.ChatLogRepository;

@Controller
public class ChatController {

    private final ChatLogRepository chatLogRepository;

    public ChatController(ChatLogRepository chatLogRepository) {
        this.chatLogRepository = chatLogRepository;
    }

    @MessageMapping("/chat/{roomId}")
    @SendTo("/topic/chat/{roomId}")
    public ChatMessage sendMessage(
            @DestinationVariable String roomId,
            @Payload ChatMessage incomingMessage,
            @AuthenticationPrincipal UserDetails userDetails) {

       String senderId = userDetails != null ? userDetails.getUsername() : "anonymous";

        Log logToSave = new Log();
        logToSave.setRoomId(roomId);
        logToSave.setUserId(senderId);
        logToSave.setLog(incomingMessage.getContent());
        logToSave.setTimestamp(LocalDateTime.now());

        Log savedLog = chatLogRepository.save(logToSave);

        ChatMessage responseMessage = new ChatMessage();
        responseMessage.setRoomId(savedLog.getRoomId());
        responseMessage.setSender(savedLog.getUserId());
        responseMessage.setContent(savedLog.getLog());
        responseMessage.setTimestamp(savedLog.getTimestamp());

        return responseMessage;
    }
}