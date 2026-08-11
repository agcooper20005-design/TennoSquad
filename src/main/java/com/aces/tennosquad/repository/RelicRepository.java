package com.aces.tennosquad.repository;

import com.aces.tennosquad.model.Relic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelicRepository extends JpaRepository<Relic, Long> {

    Optional<Relic> findByName(String name);

    List<Relic> findByActiveTrue();

    boolean existsByName(String name);

    Optional<Relic> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Relic> findByActiveFalse();
}