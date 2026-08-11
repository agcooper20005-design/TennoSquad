package com.aces.tennosquad.controller;

import com.aces.tennosquad.dto.listing.*;
import com.aces.tennosquad.service.HostListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/host-listings")
@RequiredArgsConstructor
public class HostListingController {

    private final HostListingService hostListingService;

    /** POST */

    @PostMapping
    public ResponseEntity<HostListingResponse> createListing(@Valid @RequestBody CreateHostListingRequest request) {
        HostListingResponse response =
                hostListingService.createListing(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{listingId}/join")
    public ResponseEntity<JoinHostListingResponse> joinListing(@PathVariable Long listingId) {
        return ResponseEntity.ok(hostListingService.joinListing(listingId));
    }

    @PostMapping("/{listingId}/player-count")
    public ResponseEntity<HostListingResponse> joinListingPlayerCount(@PathVariable Long listingId, @Valid @RequestBody UpdatePlayerCountRequest request) {
        return ResponseEntity.ok(hostListingService.updatePlayerCount(listingId, request));
    }



    /** PATCH */

    @PatchMapping("/{listingId}/close")
    public ResponseEntity<HostListingResponse> closeListing(@PathVariable Long listingId) {
        return ResponseEntity.ok(
                hostListingService.closeListing(listingId)
        );
    }

    @PatchMapping("/{listingId}/mission")
    public ResponseEntity<HostListingResponse> assignMission(@PathVariable Long listingId, @Valid @RequestBody AssignMissionRequest request) {
        return ResponseEntity.ok(
                hostListingService.assignMission(
                        listingId,
                        request
                )
        );
    }


    /** GET */

    @GetMapping("/{listingId}")
    public ResponseEntity<HostListingResponse> getListingById(@PathVariable Long listingId) {
        return ResponseEntity.ok(
                hostListingService.getListingById(listingId)
        );
    }

    /// get all listing by host id
    @GetMapping("/{hostId}/all")
    public ResponseEntity<List<HostListingResponse>> getListingByHostId(@PathVariable Long hostId) {
        System.out.println("******************************\n   HIT    \n**************************");
        return ResponseEntity.ok(
                hostListingService.getListingByHostId(hostId)
        );
    }

    /// get open listing by host id

    /// get full listing by host id
    @GetMapping("/{hostId}/full")
    public ResponseEntity<List<HostListingResponse>> getFullListingByHostId(@PathVariable Long hostId) {
        return ResponseEntity.ok(
                hostListingService.getFullListingByHostId(hostId)
        );
    }


    /// get all listings
    @GetMapping
    public ResponseEntity<List<HostListingResponse>> getAllListings() {
        return ResponseEntity.ok(
                hostListingService.getAllListings()
        );
    }


    /// get all open listings
    @GetMapping("/open")
    public ResponseEntity<List<HostListingResponse>> getOpenListings() {
        return ResponseEntity.ok(
                hostListingService.getOpenListings()
        );
    }

    /// get all open listings by mission id
    @GetMapping("/open/mission/{missionId}")
    public ResponseEntity<List<HostListingResponse>>
    getOpenListingsByMission(@PathVariable Long missionId) {
        return ResponseEntity.ok(
                hostListingService.getOpenListingsByMission(missionId)
        );
    }
}