package com.cheers.office.board.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.User; // ★ 不足していたimport

@RestController
public class UserController {

    @GetMapping("/api/users/me")
    public ResponseEntity<User> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            User originalUser = userDetails.getUser();
            
            // クライアントに返す用の、新しいUserオブジェクト（コピー）を作成
            User userForResponse = new User();
            userForResponse.setUserId(originalUser.getUserId());
            userForResponse.setUserName(originalUser.getUserName());
            userForResponse.setMailAddress(originalUser.getMailAddress());
            userForResponse.setIcon(originalUser.getIcon());
            userForResponse.setGroup(originalUser.getGroup());
            userForResponse.setMyBoom(originalUser.getMyBoom());
            userForResponse.setHobby(originalUser.getHobby());
            userForResponse.setStatusMessage(originalUser.getStatusMessage());
            
            return ResponseEntity.ok(userForResponse);
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}