package com.aces.tennosquad.dto.listing;

public record JoinHostListingResponse(
        Long listingId,
        String hostWarframeUserName,
        String whisperCommand,
        int playerCount,
        int maxPlayers
) {
}