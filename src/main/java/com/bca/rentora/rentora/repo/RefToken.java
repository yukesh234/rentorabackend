package com.bca.rentora.rentora.repo;

import com.bca.rentora.rentora.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefToken extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByJti(String jti);
}
