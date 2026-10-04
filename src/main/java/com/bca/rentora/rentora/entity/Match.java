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
    // display/sort only: stage * 1000 + index inside the round
    private Integer roundOrder;

    @Enumerated(EnumType.STRING)
    private BracketSide bracketSide;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    private Instant scheduledAt;

    // where the WINNER goes (set once at bracket-generation time)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_match_id")
    private Match nextMatch;

    private Integer nextMatchSlot; // 0 = teamA, 1 = teamB

    // where the LOSER goes (double elimination only; null = eliminated)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loser_next_match_id")
    private Match loserNextMatch;

    private Integer loserNextMatchSlot; // 0 = teamA, 1 = teamB

    // true when nobody will ever arrive in that slot (a bye upstream).
    // Boolean (not boolean) so ddl-auto=update can add the column to existing rows.
    private Boolean teamASlotDead;
    private Boolean teamBSlotDead;

    @Column(updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (status == null) status = MatchStatus.SCHEDULED;
    }
}