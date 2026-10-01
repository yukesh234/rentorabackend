package com.bca.rentora.rentora.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tournament_matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_a_id")
    private Team teamA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_b_id")
    private Team teamB;

    private Integer scoreA;
    private Integer scoreB;

    private String round;
    private Integer roundOrder; // display/sort only now — no longer used for advancement math

    @Enumerated(EnumType.STRING)
    private BracketSide bracketSide;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    private Instant scheduledAt;

    // NEW — explicit advancement pointer, set once at bracket-generation time.
    // Removes all runtime position-inference math.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_match_id")
    private Match nextMatch;

    private Integer nextMatchSlot; // 0 = winner goes into teamA, 1 = teamB

    @Column(updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (status == null) status = MatchStatus.SCHEDULED;
    }
}