package com.aces.tennosquad.dto.listing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdatePlayerCountRequest(

        @Min(value = 1, message = "Player count cannot be less than 1")
        @Max(value = 4, message = "Player count cannot exceed 4")
        int playerCount

) {
}
