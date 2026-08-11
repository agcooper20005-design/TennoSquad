package com.aces.tennosquad.repository;

import com.aces.tennosquad.model.RelicReward;
import com.aces.tennosquad.model.enums.RewardRarity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelicRewardRepository extends JpaRepository<RelicReward, Long> {

    List<RelicReward> findByRelicId(Long relicId);

    List<RelicReward> findByRarity(RewardRarity rarity);

    void deleteByRelicId(Long relicId);
}