package com.aces.tennosquad.controller;


import com.aces.tennosquad.dto.listing.HostListingResponse;
import com.aces.tennosquad.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/host-listing/{listingId}/force-close")
    public ResponseEntity<HostListingResponse> forceCloseListing(@PathVariable Long listingId) {

        return ResponseEntity.ok(adminService.forceCloseListing(listingId));
    }
}
