package com.aces.tennosquad.validation;

import com.aces.tennosquad.exception.InvalidListingStateException;
import com.aces.tennosquad.exception.InvalidRequestException;
import com.aces.tennosquad.model.HostListing;
import com.aces.tennosquad.model.Mission;
import com.aces.tennosquad.model.Relic;
import com.aces.tennosquad.model.User;
import com.aces.tennosquad.model.enums.HostListingStatus;
import com.aces.tennosquad.model.enums.MissionSource;
import com.aces.tennosquad.model.enums.RefinementLevel;
import com.aces.tennosquad.model.enums.RelicEra;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class HostListingValidator {

    // ==================================================
    // LEVEL 1 - ATOMIC VALIDATORS
    // Never call other validators
    // ==================================================


    public boolean MissionExists(HostListing listing){
        return listing.getMission() != null;
    }

    /// checks player capacity, max players can't be greater than 4, minimum players can't be less than 1.
    public void validatePlayerCapacity(
            int playerCount,
            int maxPlayers
    ) {
        if (playerCount < 1) {
            throw new InvalidRequestException(
                    "Player count must be at least 1"
            );
        }

        if (maxPlayers < 1 || maxPlayers > 4) {
            throw new InvalidRequestException(
                    "Maximum players must be between 1 and 4"
            );
        }

        if (playerCount > maxPlayers) {
            throw new InvalidRequestException(
                    "Player count cannot exceed maximum players"
            );
        }
    }

    public void validateListingIsNotClosed(
            HostListing listing
    ) {
        if (listing.getStatus() == HostListingStatus.CLOSED) {
            throw new InvalidListingStateException(
                    "The listing is closed"
            );
        }
    }

    public void validateListingHasCapacity(
            HostListing listing
    ) {
        if (listing.getStatus() == HostListingStatus.FULL
                || listing.getPlayerCount() >= listing.getMaxPlayers()) {

            throw new InvalidListingStateException(
                    "The listing is full"
            );
        }
    }

    /// Level 1
    public void validateMissionIsActive(
            Mission mission
    ) {
        boolean expired =
                mission.getExpiry() != null
                        && !mission.getExpiry().isAfter(Instant.now());

        if (!mission.isActive() || expired) {
            throw new InvalidListingStateException(
                    "The selected mission is no longer active"
            );
        }
    }

    /// Level 1
    public void validateMissionMatchesSource(
            Mission mission,
            MissionSource source
    ) {
        if (mission.getMissionSource() != source) {
            throw new InvalidRequestException(
                    "Selected mission does not match listing source"
            );
        }
    }

    public void validateRelicRefinement(
            Relic relic,
            RefinementLevel refinementLevel
    ) {
        if (relic != null && refinementLevel == null) {
            throw new InvalidRequestException(
                    "A relic listing must specify a refinement level"
            );
        }

        if (relic == null && refinementLevel != null) {
            throw new InvalidRequestException(
                    "Refinement level cannot be specified without a relic"
            );
        }
    }

    /// Level 1
    public void validateRelicMatchesMission(
            Relic relic,
            Mission mission
    ) {
        if (mission.getMissionSource() != MissionSource.FISSURE) {
            throw new InvalidRequestException(
                    "A relic can only be assigned to a fissure mission"
            );
        }

        if (mission.getFissureEra() == null) {
            throw new InvalidRequestException(
                    "The selected fissure has no relic era"
            );
        }

        if (mission.getFissureEra() == RelicEra.OMNIA) {
            return;
        }

        if (relic.getRelicEra() != mission.getFissureEra()) {
            throw new InvalidRequestException(
                    relic.getRelicEra()
                            + " relics cannot be used in a "
                            + mission.getFissureEra()
                            + " fissure"
            );
        }
    }

    /// Level 1
    public void validateListingOwnership(HostListing listing, User authenticatedUser){
        System.out.printf("Listing id: %d\nHost id: %s\nUser id: %d\n", listing.getId(), listing.getHost().getId(), authenticatedUser.getId());
        if(!listing.getHost().getId().equals(authenticatedUser.getId())){
            throw new AccessDeniedException("You do not own this listing");

        }
    }


    // ==================================================
    // LEVEL 2 - DOMAIN VALIDATORS
    // These combine Level 1 validators
    // ==================================================

    public void validateFissureListing(
            Mission mission,
            Relic relic,
            RefinementLevel refinementLevel
    ) {
        if (mission == null && relic == null) {
            throw new InvalidRequestException(
                    "A fissure listing must specify a mission or relic"
            );
        }

        validateRelicRefinement(
                relic,
                refinementLevel
        );

        if (mission != null) {
            validateMissionIsActive(mission);
            validateMissionMatchesSource(
                    mission,
                    MissionSource.FISSURE
            );
        }

        if (mission != null && relic != null) {
            validateRelicMatchesMission(
                    relic,
                    mission
            );
        }
    }

    public void validateMissionExistsAndIsActive(HostListing listing){
        if(listing.getMission() != null){
            validateMissionIsActive(listing.getMission());
        }
    }

    public void validateRelicExistsAndMatchesMission(HostListing listing){

        if(listing.getRelic() != null){
            if(MissionExists(listing)) {
                validateRelicMatchesMission(listing.getRelic(),listing.getMission());
            }
        }
    }

    public void validateArbitrationListing(
            Mission mission,
            Relic relic,
            RefinementLevel refinementLevel
    ) {
        if (mission != null) {
            throw new InvalidRequestException(
                    "Arbitration listings do not use a specific mission"
            );
        }

        if (relic != null) {
            throw new InvalidRequestException(
                    "Arbitration listings cannot specify a relic"
            );
        }

        if (refinementLevel != null) {
            throw new InvalidRequestException(
                    "Arbitration listings cannot specify a refinement level"
            );
        }
    }


    // ==================================================
    // LEVEL 3 - WORKFLOW VALIDATORS
    // HostListingService should call these
    // ==================================================


    public void validateListingCreation(
            int playerCount,
            int maxPlayers,
            Mission mission,
            Relic relic,
            RefinementLevel refinementLevel,
            MissionSource missionSource
    ) {
        validatePlayerCapacity(
                playerCount,
                maxPlayers
        );

        if (missionSource == null) {
            throw new InvalidRequestException(
                    "Mission source is required"
            );
        }

        switch (missionSource) {
            case FISSURE ->
                    validateFissureListing(
                            mission,
                            relic,
                            refinementLevel
                    );

            case ARBITRATION ->
                    validateArbitrationListing(
                            mission,
                            relic,
                            refinementLevel
                    );
        }
    }

    /// Validates ownership, and validates listing is not closed
    public void validateListingCanChangePlayerCount(
            HostListing listing,
            User authenticatedUser
    ) {
        validateListingOwnership(listing, authenticatedUser);
        validateListingIsNotClosed(listing);

    }

    /// For Create Listing
    /// Validates listing is not closed, checks if the mission is active
    public void validateListingCanBeModified(
            HostListing listing
    ) {
        validateListingIsNotClosed(listing);

        if (listing.getMission() != null) {
            validateMissionIsActive(
                    listing.getMission()
            );
        }
    }


    /// For assignMission method
    public void validateListingCanBeAssignedMission(User authenticatedUser, Mission mission, HostListing listing){
        validateListingOwnership(listing,authenticatedUser);
        validateListingIsNotClosed(listing);
        validateMissionIsActive(mission);
        validateMissionMatchesSource(mission, listing.getMissionSource());


        if(listing.getRelic() != null){
            validateRelicMatchesMission(listing.getRelic(),mission);
        }

    }
    public void validateListingCanBeClosed(
            HostListing listing,
            User authenticatedUser
    ) {

        validateListingOwnership(listing, authenticatedUser);
        validateListingIsNotClosed(listing);
    }

    public void validateListingCanReceiveJoinRequest(
            HostListing listing
    ) {
        validateListingIsNotClosed(listing);
        validateListingHasCapacity(listing);

        if (listing.getMission() != null) {
            validateMissionIsActive(
                    listing.getMission()
            );
        }
    }
}