package com.bca.rentora.rentora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "refreshtokens", indexes = {
        @Index(name = "refresh_token_jti",columnList = "jti",unique = true),
        @Index(name="refresh_Token_userId",columnList = "user_id")
})
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true,updatable = false)
    private String jti;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    private boolean revoked;
    private String replacedByToken;

}
