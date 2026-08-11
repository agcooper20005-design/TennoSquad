package com.aces.tennosquad.model;

import com.aces.tennosquad.model.enums.HostListingStatus;
import com.aces.tennosquad.model.enums.MissionSource;
import com.aces.tennosquad.model.enums.RefinementLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "host_listings")
@Getter
@Setter
@NoArgsConstructor
public class HostListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionSource missionSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relic_id")
    private Relic relic;

    @Enumerated(EnumType.STRING)
    @Column
    private RefinementLevel refinementLevel;

    @Column(nullable = false)
    private int playerCount;

    @Column(nullable = false)
    private int maxPlayers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HostListingStatus status;

    private Integer expectedDurationMinutes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();

        if (playerCount == 0) {
            playerCount = 1;
        }

        if (maxPlayers == 0) {
            maxPlayers = 4;
        }

        if (status == null) {
            status = HostListingStatus.OPEN;
        }
    }
}