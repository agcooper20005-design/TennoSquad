package com.aces.tennosquad.controller;


import com.aces.tennosquad.dto.relic.CreateRelicRequest;
import com.aces.tennosquad.dto.relic.RelicResponse;
import com.aces.tennosquad.integration.WarframeSyncService;
import com.aces.tennosquad.model.Relic;
import com.aces.tennosquad.service.HostListingService;
import com.aces.tennosquad.service.MissionService;
import com.aces.tennosquad.service.RelicPullScriptService;
import com.aces.tennosquad.service.RelicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final WarframeSyncService warframeSyncService;
    private final RelicPullScriptService relicPullScriptService;
    private final RelicService relicService;
    private final HostListingService hostListingService;
    private final MissionService missionService;

    @PostMapping("/missions/sync")
    public void syncMissions() {
        warframeSyncService.syncCurrentMissions();
    }


    @PostMapping("/relics/fetch")
    public List<Relic> fetchRelics() {
        return relicPullScriptService.pullRelics();
    }

    @PatchMapping("/deactivate-expired")
    public ResponseEntity<Integer> deactivateExpiredMissions() {
        return ResponseEntity.ok(
                missionService.deactivateExpiredMissions()
        );
    }

    @DeleteMapping("/inactive")
    public ResponseEntity<Integer> deleteInactiveMissions() {
        return ResponseEntity.ok(
                missionService.deleteInactiveMissions()
        );
    }

    @PostMapping("/host-listing/close-inactive")
    public ResponseEntity<Integer> closeListingsForInactiveMissions() {
        return ResponseEntity.ok(
                hostListingService.closeListingsForInactiveMissions()
        );
    }

}
