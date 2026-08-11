package com.aces.tennosquad.model;

import com.aces.tennosquad.model.enums.RewardRarity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "relic_rewards")
@Getter
@Setter
@NoArgsConstructor
public class RelicReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relic_id", nullable = false)
    private Relic relic;

    @Column(nullable = false)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardRarity rarity;

    @Column(
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal chance;
}