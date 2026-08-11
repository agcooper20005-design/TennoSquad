package com.aces.tennosquad.controller;

import com.aces.tennosquad.dto.user.CreateUserRequest;
import com.aces.tennosquad.dto.user.UserResponse;
import com.aces.tennosquad.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/// CONTROLLER PROTECTED | NEEDED ROLE: ADMIN
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createUser(request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                userService.getUserById(userId)
        );
    }

    @GetMapping("/username/{userName}")
    public ResponseEntity<UserResponse> getUserByUserName(
            @PathVariable String userName
    ) {
        return ResponseEntity.ok(
                userService.getUserByUserName(userName)
        );
    }

    @GetMapping("/warframe-username/{warframeUserName}")
    public ResponseEntity<UserResponse> getUserByWarframeUserName(
            @PathVariable String warframeUserName
    ) {
        return ResponseEntity.ok(
                userService.getUserByWarframeUserName(
                        warframeUserName
                )
        );
    }
}