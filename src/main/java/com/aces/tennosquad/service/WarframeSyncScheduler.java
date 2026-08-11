package com.aces.tennosquad.service;


import com.aces.tennosquad.integration.WarframeSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WarframeSyncScheduler {

    private final WarframeSyncService warframeSyncService;

    @Scheduled(fixedRate = 30000)
    public void syncWarframeData(){
        warframeSyncService.syncCurrentMissions();
    }


}
