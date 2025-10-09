package com.cheers.office.board.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cheers.office.board.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class UserRestController {

    private static final String USER_PATH = "src/main/resources/data/user.json";
    private final ObjectMapper mapper = new ObjectMapper();

    // --- 全ユーザー一覧を取得 ---
    @GetMapping("/users")
    public List<User> getUsers() throws IOException {
        File file = new File(USER_PATH);
        if (!file.exists()) return List.of();
        return mapper.readValue(file, new TypeReference<List<User>>() {});
    }

    // --- ログイン中ユーザーを返す ---
    @GetMapping("/sessionUser")
    public User getSessionUser(@AuthenticationPrincipal UserDetails userDetails) throws IOException {
        if (userDetails == null) return null;

        File file = new File(USER_PATH);
        if (!file.exists()) return null;
        List<User> users = mapper.readValue(file, new TypeReference<List<User>>() {});

        Optional<User> found = users.stream()
                .filter(u -> u.getMailAddress().equals(userDetails.getUsername()))
                .findFirst();

        return found.orElse(null);
    }
}
