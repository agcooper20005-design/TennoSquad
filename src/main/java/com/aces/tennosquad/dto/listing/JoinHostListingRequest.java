package com.aces.tennosquad.dto.listing;

import jakarta.validation.constraints.NotNull;

public record JoinHostListingRequest(
        @NotNull(message = "Listing ID is required")
        Long listingId
) {
}