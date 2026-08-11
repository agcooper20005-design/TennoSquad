package com.aces.tennosquad.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.aspectj.lang.annotation.RequiredTypes;

public record CreateUserRequest(

        @NotBlank(message = "Username is required")
        @Size(
                min = 3,
                max = 30,
                message = "Username must be between 3 and 30 characters"
        )
        String userName,

        @NotBlank(message = "Warframe username is required")
        @Size(
                max = 50,
                message = "Warframe username cannot exceed 50 characters"
        )
        String warframeUserName,

        @NotBlank(message = "Password is Required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Size(max = 100, message = "Password cannot exceed 100 characters")
        String password

) {
}