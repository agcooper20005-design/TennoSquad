package com.aces.tennosquad.repository;

import com.aces.tennosquad.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserName(String userName);

    Optional<User> findByUserNameIgnoreCase(String userName);

    Optional<User> findByWarframeUserNameIgnoreCase(
            String warframeUserName
    );

    boolean existsByUserNameIgnoreCase(String userName);

    boolean existsByWarframeUserNameIgnoreCase(
            String warframeUserName
    );
}