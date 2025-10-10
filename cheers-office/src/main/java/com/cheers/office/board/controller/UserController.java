package com.cheers.office.board.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cheers.office.board.model.CustomUserDetails;
import com.cheers.office.board.model.User;
import com.cheers.office.board.repository.UserRepository;

@RestController
public class UserController {

    // The UserRepository is injected here.
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // This is the correct endpoint for the current user.
    @GetMapping("/api/users/me")
    public ResponseEntity<User> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            User originalUser = userDetails.getUser();
            
            // Creates a safe User object to return to the client.
            User userForResponse = new User();
            userForResponse.setUserId(originalUser.getUserId());
            userForResponse.setUserName(originalUser.getUserName());
            userForResponse.setMailAddress(originalUser.getMailAddress());
            userForResponse.setIcon(originalUser.getIcon());
            userForResponse.setGroup(originalUser.getGroup());
            userForResponse.setMyBoom(originalUser.getMyBoom());
            userForResponse.setHobby(originalUser.getHobby());
            userForResponse.setStatusMessage(originalUser.getStatusMessage());
            userForResponse.setTeamColor(originalUser.getTeamColor());
            
            return ResponseEntity.ok(userForResponse);
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // This method correctly gets all users from the repository.
    @GetMapping("/api/users")
    public List<User> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> {
                    // Returns a safe object without sensitive information like passwords.
                    User safeUser = new User();
                    safeUser.setUserId(user.getUserId());
                    safeUser.setUserName(user.getUserName());
                    safeUser.setIcon(user.getIcon());
                    safeUser.setTeamColor(user.getTeamColor());
                    return safeUser;
                })
                .collect(Collectors.toList());
    }
}