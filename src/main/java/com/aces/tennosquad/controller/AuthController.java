package com.aces.tennosquad.controller;


import com.aces.tennosquad.dto.login.LoginRequest;
import com.aces.tennosquad.dto.user.MeResponse;
import com.aces.tennosquad.security.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        authService.login(request, httpRequest, httpResponse);

        return ResponseEntity.ok().build();
    }


    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {

        return ResponseEntity.ok(authService.getCurrentUser(authentication));

    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfToken> csrf(CsrfToken csrfToken) {

        String token = csrfToken.getToken();


        return ResponseEntity.ok(csrfToken);
    }

}
