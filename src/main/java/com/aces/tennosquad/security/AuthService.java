package com.aces.tennosquad.security;

import com.aces.tennosquad.dto.login.LoginRequest;
import com.aces.tennosquad.dto.user.MeResponse;
import com.aces.tennosquad.dto.user.UserResponse;
import com.aces.tennosquad.exception.ResourceNotFoundException;
import com.aces.tennosquad.model.User;
import com.aces.tennosquad.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();


    public void login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {

        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.userName(),
                        request.password()
                );

        Authentication authentication =
                authenticationManager.authenticate(
                        authenticationRequest
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );
    }


    @Transactional(readOnly = true)
    public MeResponse getCurrentUser(
            Authentication authentication
    ) {

        User user =
                userRepository
                        .findByUserName(
                                authentication.getName()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Authenticated user not found"
                                )
                        );

        return mapToResponse(user);
    }


    private MeResponse mapToResponse(
            User user
    ) {
        return new MeResponse(
                user.getId(),
                user.getUserName(),
                user.getWarframeUserName(),
                user.getRole()
        );
    }
}