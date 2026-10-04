package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.Tournament.*;
import com.bca.rentora.rentora.entity.*;
import com.bca.rentora.rentora.exceptions.ResourceNotFoundException;
import com.bca.rentora.rentora.repo.BookingRepo;
import com.bca.rentora.rentora.repo.MatchRepo;
import com.bca.rentora.rentora.repo.TeamRepo;
import com.bca.rentora.rentora.repo.TournamentRepo;
import com.bca.rentora.rentora.repo.UserRepo;
import com.bca.rentora.rentora.services.TournamentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepo tournamentRepo;
    private final TeamRepo teamRepo;
    private final MatchRepo matchRepo;
    private final BookingRepo bookingRepo;
    private final UserRepo userRepo;

    public TournamentServiceImpl(TournamentRepo tournamentRepo, TeamRepo teamRepo,
                                 MatchRepo matchRepo, BookingRepo bookingRepo, UserRepo userRepo) {
        this.tournamentRepo = tournamentRepo;
        this.teamRepo = teamRepo;
        this.matchRepo = matchRepo;
        this.bookingRepo = bookingRepo;
        this.userRepo = userRepo;
    }

    // ---------------- Tournament CRUD ----------------

    @Override
    @Transactional
    public TournamentResponseDto createTournament(TournamentCreateDto dto, UUID organizerId) {
        Booking booking = bookingRepo.findById(dto.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getUserid().equals(organizerId)) {
            throw new IllegalArgumentException("You can only create a tournament on your own booking");
        }

        if (booking.getListing().getCategory() != CategoryType.SPORTS) {
            throw new IllegalArgumentException("Tournaments can only be created for sports listings");
        }

        if (tournamentRepo.findByBooking_Id(dto.bookingId()).isPresent()) {
            throw new IllegalArgumentException("A tournament already exists for this booking");
        }

        Tournament tournament = new Tournament();
        tournament.setBooking(booking);
        tournament.setName(dto.name());
        tournament.setSportType(dto.sportType());
        tournament.setFormat(dto.format());
        tournament.setMaxTeams(dto.maxTeams());

        tournamentRepo.save(tournament);

        return toDto(tournament);
    }

    @Override
    public TournamentResponseDto getTournament(UUID tournamentId) {
        Tournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found"));
        return toDto(tournament);
    }

    @Override
    public List<TournamentResponseDto> getBrowsableTournaments() {
        return tournamentRepo.findAll()
                .stream()
                .filter(t -> t.getStatus() != TournamentStatus.CANCELLED)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ---------------- Teams (B3) ----------------

    @Override
    @Transactional
    public TeamResponseDto addTeam(UUID tournamentId, TeamCreateDto dto, UUID userId) {
        Tournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found"));

        if (tournament.getStatus() != TournamentStatus.PLANNED) {
            throw new IllegalArgumentException("Teams can only be added while the tournament is still being planned");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isOrganizer = tournament.getBooking().getUser().getUserid().equals(userId);
        List<Team> existing = teamRepo.findByTournament_Id(tournamentId);

        if (existing.size() >= tournament.getMaxTeams()) {
            throw new IllegalArgumentException("Maximum number of teams already reached");
        }

        String name = dto.name().trim();
        if (existing.stream().anyMatch(t -> t.getName() != null && t.getName().equalsIgnoreCase(name))) {
            throw new IllegalArgumentException("A team with this name is already registered");
        }

        // the organizer can add several teams; everyone else gets one team each
        if (!isOrganizer && existing.stream().anyMatch(t ->
                t.getRegisteredBy() != null && t.getRegisteredBy().getUserid().equals(userId))) {
            throw new IllegalArgumentException("You have already registered a team in this tournament");
        }

        Team team = new Team();
        team.setTournament(tournament);
        team.setName(name);
        team.setRegisteredBy(user);
        teamRepo.save(team);

        return toTeamDto(team);
    }

    @Override
    @Transactional
    public void removeTeam(UUID tournamentId, UUID teamId, UUID userId) {
        Tournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found"));

        if (tournament.getStatus() != TournamentStatus.PLANNED) {
            throw new IllegalArgumentException("Teams can only be removed before the bracket is generated");
        }

        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        if (!team.getTournament().getId().equals(tournamentId)) {
            throw new IllegalArgumentException("This team does not belong to this tournament");
        }

        boolean isOrganizer = tournament.getBooking().getUser().getUserid().equals(userId);
        boolean isOwnTeam = team.getRegisteredBy() != null && team.getRegisteredBy().getUserid().equals(userId);
        if (!isOrganizer && !isOwnTeam) {
            throw new IllegalArgumentException("You can only remove your own team");
        }

        teamRepo.delete(team);
    }

    @Override
    public List<TeamResponseDto> getTeams(UUID tournamentId) {
        return teamRepo.findByTournament_Id(tournamentId)
                .stream()
                .map(this::toTeamDto)
                .collect(Collectors.toList());
    }

    // ---------------- Bracket generation (B5) ----------------

    @Override
    @Transactional
    public List<MatchResponseDto> generateBracket(UUID tournamentId, UUID organizerId) {
        Tournament tournament = getOwnedTournament(tournamentId, organizerId);

        if (tournament.getStatus() != TournamentStatus.PLANNED) {
            throw new IllegalArgumentException("Bracket has already been generated for this tournament");
        }

        List<Team> teams = teamRepo.findByTournament_Id(tournamentId);
        if (teams.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 teams to generate a bracket");
        }
        if (tournament.getFormat() == TournamentFormat.DOUBLE_ELIMINATION && teams.size() < 3) {
            throw new IllegalArgumentException("Double elimination needs at least 3 teams");
        }

        List<Match> matches = switch (tournament.getFormat()) {
            case KNOCKOUT -> generateKnockout(tournament, teams);
            case DOUBLE_ELIMINATION -> generateDoubleElimination(tournament, teams);
            case ROUND_ROBIN -> generateRoundRobin(tournament, teams, false);
            case LEAGUE -> generateRoundRobin(tournament, teams, true);
        };

        matchRepo.saveAll(matches);

        tournament.setStatus(TournamentStatus.ONGOING);
        tournamentRepo.save(tournament);

        return matches.stream().map(this::toMatchDto).collect(Collectors.toList());
    }

    // ----- knockout -----

    private List<Match> generateKnockout(Tournament tournament, List<Team> teams) {
        List<Team> shuffled = new ArrayList<>(teams);
        Collections.shuffle(shuffled);

        int size = bracketSize(shuffled.size());
        List<Team> seeded = seedWithByes(shuffled, size);

        List<List<Match>> rounds = buildWinnersRounds(tournament, seeded, size, false);
        resolveFirstRoundByes(rounds.get(0));

        return rounds.stream().flatMap(List::stream).collect(Collectors.toList());
    }

    // ----- double elimination -----
    //
    // Winners bracket: W1..Wk. Losers bracket: 2k-2 rounds. Round 1 pairs the W1 losers;
    // after that, every winners round drops its losers into a "drop" round (L winners vs
    // W losers), followed by a "consolidation" round where the L winners pair up.
    // Winners final winner -> Grand Final slot A, losers final winner -> slot B.
    // Single grand final (no bracket reset).

    private List<Match> generateDoubleElimination(Tournament tournament, List<Team> teams) {
        List<Team> shuffled = new ArrayList<>(teams);
        Collections.shuffle(shuffled);

        int size = bracketSize(shuffled.size()); // >= 4 because teams >= 3
        List<Team> seeded = seedWithByes(shuffled, size);

        List<List<Match>> winners = buildWinnersRounds(tournament, seeded, size, true);
        List<List<Match>> losers = buildLosersRounds(tournament, winners, size);

        Match grandFinal = newMatch(tournament, "Grand Final", 1000, BracketSide.GRAND_FINAL);

        Match winnersFinal = winners.get(winners.size() - 1).get(0);
        Match losersFinal = losers.get(losers.size() - 1).get(0);
        winnersFinal.setNextMatch(grandFinal);
        winnersFinal.setNextMatchSlot(0);
        losersFinal.setNextMatch(grandFinal);
        losersFinal.setNextMatchSlot(1);

        resolveFirstRoundByes(winners.get(0));

        List<Match> all = new ArrayList<>();
        winners.forEach(all::addAll);
        losers.forEach(all::addAll);
        all.add(grandFinal);
        return all;
    }

    private List<List<Match>> buildLosersRounds(Tournament tournament, List<List<Match>> winners, int size) {
        int k = winners.size();
        int totalRounds = 2 * k - 2;
        List<List<Match>> losers = new ArrayList<>();
        int stage = 1;

        // L1: losers of the first winners round play each other
        List<Match> current = newLosersRound(tournament, stage++, totalRounds, size / 4);
        losers.add(current);
        List<Match> w1 = winners.get(0);
        for (int i = 0; i < w1.size(); i++) {
            w1.get(i).setLoserNextMatch(current.get(i / 2));
            w1.get(i).setLoserNextMatchSlot(i % 2);
        }

        for (int r = 2; r <= k; r++) {
            // drop round: previous losers-round winners vs losers of winners round r
            List<Match> drop = newLosersRound(tournament, stage++, totalRounds, size >> r);
            losers.add(drop);
            for (int i = 0; i < current.size(); i++) {
                current.get(i).setNextMatch(drop.get(i));
                current.get(i).setNextMatchSlot(0);
            }
            List<Match> wr = winners.get(r - 1);
            for (int i = 0; i < wr.size(); i++) {
                wr.get(i).setLoserNextMatch(drop.get(i));
                wr.get(i).setLoserNextMatchSlot(1);
            }
            current = drop;

            // consolidation round: drop-round winners pair up (not needed after the last drop)
            if (r < k) {
                List<Match> cons = newLosersRound(tournament, stage++, totalRounds, size >> (r + 1));
                losers.add(cons);
                for (int i = 0; i < current.size(); i++) {
                    current.get(i).setNextMatch(cons.get(i / 2));
                    current.get(i).setNextMatchSlot(i % 2);
                }
                current = cons;
            }
        }
        return losers;
    }

    private List<Match> newLosersRound(Tournament tournament, int stage, int totalRounds, int count) {
        String name = stage == totalRounds ? "Losers Final" : "Losers Round " + stage;
        List<Match> round = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            round.add(newMatch(tournament, name, stage * 1000 + i, BracketSide.LOSERS));
        }
        return round;
    }

    // ----- shared winners-bracket builder (knockout + double elimination) -----

    private List<List<Match>> buildWinnersRounds(Tournament tournament, List<Team> seeded, int size, boolean doubleElim) {
        int k = Integer.numberOfTrailingZeros(size); // size is a power of two
        List<List<Match>> rounds = new ArrayList<>();

        for (int r = 1; r <= k; r++) {
            String name;
            if (doubleElim) {
                name = r == k ? "Winners Final" : "Winners Round " + r;
            } else {
                name = r == k ? "Final" : (r == k - 1 ? "Semifinal" : "Round " + r);
            }

            int count = size >> r;
            List<Match> round = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Match m = newMatch(tournament, name, r * 1000 + i, doubleElim ? BracketSide.WINNERS : null);
                if (r == 1) {
                    m.setTeamA(seeded.get(2 * i));
                    m.setTeamB(seeded.get(2 * i + 1));
                }
                round.add(m);
            }
            rounds.add(round);
        }

        wireSequentialRounds(rounds);
        return rounds;
    }

    // match i and i+1 in round r both feed match i/2 in round r+1 (even -> teamA, odd -> teamB)
    private void wireSequentialRounds(List<List<Match>> rounds) {
        for (int r = 0; r < rounds.size() - 1; r++) {
            List<Match> current = rounds.get(r);
            List<Match> next = rounds.get(r + 1);
            for (int i = 0; i < current.size(); i++) {
                current.get(i).setNextMatch(next.get(i / 2));
                current.get(i).setNextMatchSlot(i % 2);
            }
        }
    }

    private int bracketSize(int teamCount) {
        int size = 1;
        while (size < teamCount) size *= 2;
        return size;
    }

    /**
     * Places teams into round-1 slots so that a bye is always paired with a real team
     * (never bye vs bye), with the bye matches spread across the bracket.
     */
    private List<Team> seedWithByes(List<Team> shuffled, int size) {
        int matchCount = size / 2;
        int byes = size - shuffled.size();

        Set<Integer> byeMatches = new HashSet<>();
        for (int j = 0; j < byes; j++) {
            byeMatches.add(j * matchCount / byes); // byes < matchCount, so these are distinct
        }

        List<Team> seeded = new ArrayList<>(size);
        int next = 0;
        for (int i = 0; i < matchCount; i++) {
            seeded.add(shuffled.get(next++));
            seeded.add(byeMatches.contains(i) ? null : shuffled.get(next++));
        }
        return seeded;
    }

    private void resolveFirstRoundByes(List<Match> firstRound) {
        for (Match m : firstRound) {
            if (m.getTeamA() == null || m.getTeamB() == null) {
                if (m.getTeamA() == null) m.setTeamASlotDead(true);
                else m.setTeamBSlotDead(true);
                resolveBye(m);
            }
        }
    }

    private Match newMatch(Tournament tournament, String round, int roundOrder, BracketSide side) {
        Match m = new Match();
        m.setTournament(tournament);
        m.setRound(round);
        m.setRoundOrder(roundOrder);
        m.setBracketSide(side);
        m.setStatus(MatchStatus.SCHEDULED);
        return m;
    }

    // ---------------- Advancement (winner / loser delivery with bye handling) ----------------

    /**
     * Sends a team (or "nobody", when team == null) into a slot of the target match.
     * If the other slot of that match can never be filled, the match is resolved as a bye
     * and whoever is in it moves on straight away.
     */
    private void deliver(Match target, Integer slot, Team team) {
        if (target == null || slot == null) return;

        boolean isA = slot == 0;
        if (team == null) {
            if (isA) target.setTeamASlotDead(true);
            else target.setTeamBSlotDead(true);
        } else {
            if (isA) target.setTeamA(team);
            else target.setTeamB(team);
        }

        boolean aDead = Boolean.TRUE.equals(target.getTeamASlotDead());
        boolean bDead = Boolean.TRUE.equals(target.getTeamBSlotDead());
        boolean aHas = target.getTeamA() != null;
        boolean bHas = target.getTeamB() != null;

        if (aDead && bDead) {
            // nobody will ever play here: pass "nobody" downstream
            target.setStatus(MatchStatus.FORFEIT);
            deliver(target.getNextMatch(), target.getNextMatchSlot(), null);
            deliver(target.getLoserNextMatch(), target.getLoserNextMatchSlot(), null);
        } else if ((aDead && bHas) || (bDead && aHas)) {
            resolveBye(target);
        }
    }

    // exactly one real team is in this match: it advances, and there is no loser to drop
    private void resolveBye(Match m) {
        Team only = m.getTeamA() != null ? m.getTeamA() : m.getTeamB();
        m.setStatus(MatchStatus.FORFEIT);
        deliver(m.getNextMatch(), m.getNextMatchSlot(), only);
        deliver(m.getLoserNextMatch(), m.getLoserNextMatchSlot(), null);
    }

    // ----- round robin / league -----
    // Round robin: everyone plays everyone once. League: home and away (two legs).
    // Uses the circle method so every team plays exactly once per matchday.

    private List<Match> generateRoundRobin(Tournament tournament, List<Team> teams, boolean doubleLeg) {
        List<Team> list = new ArrayList<>(teams);
        Collections.shuffle(list);
        if (list.size() % 2 == 1) list.add(null); // null = the team resting that matchday

        int n = list.size();
        int roundsPerLeg = n - 1;
        List<Match> matches = new ArrayList<>();
        int order = 0;

        for (int leg = 1; leg <= (doubleLeg ? 2 : 1); leg++) {
            List<Team> rotation = new ArrayList<>(list);
            for (int r = 0; r < roundsPerLeg; r++) {
                int roundNumber = (leg - 1) * roundsPerLeg + r + 1;
                String name = (doubleLeg ? "Matchday " : "Round ") + roundNumber;

                for (int i = 0; i < n / 2; i++) {
                    Team a = rotation.get(i);
                    Team b = rotation.get(n - 1 - i);
                    if (a == null || b == null) continue;
                    if (leg == 2) { Team tmp = a; a = b; b = tmp; } // swap home/away

                    Match m = newMatch(tournament, name, order++, null);
                    m.setTeamA(a);
                    m.setTeamB(b);
                    matches.add(m);
                }

                // keep the first team fixed, rotate the rest
                Team last = rotation.remove(n - 1);
                rotation.add(1, last);
            }
        }
        return matches;
    }

    // ---------------- Matches ----------------

    @Override
    public List<MatchResponseDto> getMatches(UUID tournamentId) {
        return matchRepo.findByTournament_IdOrderByRoundOrderAsc(tournamentId)
                .stream()
                .map(this::toMatchDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MatchResponseDto updateScore(UUID matchId, MatchScoreUpdateDto dto, UUID organizerId) {
        Match match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));

        Tournament tournament = match.getTournament();
        if (!tournament.getBooking().getUser().getUserid().equals(organizerId)) {
            throw new IllegalArgumentException("Only the organizer can update scores");
        }

        if (match.getTeamA() == null || match.getTeamB() == null) {
            throw new IllegalArgumentException("Both teams must be set before recording a score");
        }

        boolean elimination = tournament.getFormat() == TournamentFormat.KNOCKOUT
                || tournament.getFormat() == TournamentFormat.DOUBLE_ELIMINATION;

        if (elimination) {
            // A10: someone has to win, otherwise nobody knows who advances
            if (dto.scoreA().equals(dto.scoreB())) {
                throw new IllegalArgumentException(
                        "Draws are not allowed in knockout formats. Enter the final winning score (e.g. after extra time or penalties).");
            }
            // re-scoring would push a second winner into the next round
            if (match.getStatus() == MatchStatus.FINISHED) {
                throw new IllegalArgumentException("This match already has a result and the winner has advanced");
            }
        }

        match.setScoreA(dto.scoreA());
        match.setScoreB(dto.scoreB());
        match.setStatus(MatchStatus.FINISHED);
        matchRepo.save(match);

        if (elimination) {
            boolean aWins = dto.scoreA() > dto.scoreB();
            Team winner = aWins ? match.getTeamA() : match.getTeamB();
            Team loser = aWins ? match.getTeamB() : match.getTeamA();

            deliver(match.getNextMatch(), match.getNextMatchSlot(), winner);
            // double elimination only: the loser drops to the losers bracket (null pointer = eliminated)
            deliver(match.getLoserNextMatch(), match.getLoserNextMatchSlot(), loser);
        }

        // the query below flushes the changes made above first
        List<Match> allMatches = matchRepo.findByTournament_IdOrderByRoundOrderAsc(tournament.getId());
        boolean allDone = elimination
                ? allMatches.stream().allMatch(m -> m.getStatus() == MatchStatus.FINISHED || m.getStatus() == MatchStatus.FORFEIT)
                : allMatches.stream().allMatch(m -> m.getStatus() == MatchStatus.FINISHED);
        if (allDone) {
            tournament.setStatus(TournamentStatus.COMPLETED);
            tournamentRepo.save(tournament);
        }

        return toMatchDto(match);
    }

    @Override
    @Transactional
    public MatchResponseDto updateSchedule(UUID matchId, MatchScheduleUpdateDto dto, UUID organizerId) {
        Match match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));

        if (!match.getTournament().getBooking().getUser().getUserid().equals(organizerId)) {
            throw new IllegalArgumentException("Only the organizer can schedule matches");
        }

        if (match.getStatus() == MatchStatus.FINISHED || match.getStatus() == MatchStatus.FORFEIT) {
            throw new IllegalArgumentException("This match is already decided and can't be rescheduled");
        }

        match.setScheduledAt(dto.scheduledAt());
        matchRepo.save(match);

        return toMatchDto(match);
    }

    // ---------------- Standings (round robin / league) ----------------

    @Override
    public List<StandingRowDto> getStandings(UUID tournamentId) {
        List<Team> teams = teamRepo.findByTournament_Id(tournamentId);
        List<Match> matches = matchRepo.findByTournament_IdOrderByRoundOrderAsc(tournamentId);

        Map<UUID, int[]> stats = new HashMap<>(); // [played, wins, losses, draws, points]
        for (Team t : teams) stats.put(t.getId(), new int[5]);

        for (Match m : matches) {
            if (m.getStatus() != MatchStatus.FINISHED || m.getTeamA() == null || m.getTeamB() == null) continue;

            int[] a = stats.get(m.getTeamA().getId());
            int[] b = stats.get(m.getTeamB().getId());
            if (a == null || b == null) continue;

            a[0]++; b[0]++;
            if (m.getScoreA() > m.getScoreB()) {
                a[1]++; a[4] += 3;
                b[2]++;
            } else if (m.getScoreB() > m.getScoreA()) {
                b[1]++; b[4] += 3;
                a[2]++;
            } else {
                a[3]++; a[4] += 1;
                b[3]++; b[4] += 1;
            }
        }

        return teams.stream()
                .map(t -> {
                    int[] s = stats.get(t.getId());
                    return new StandingRowDto(t.getId(), t.getName(), s[0], s[1], s[2], s[3], s[4]);
                })
                // points first, then most wins as the tie-break
                .sorted(Comparator.comparingInt(StandingRowDto::points)
                        .thenComparingInt(StandingRowDto::wins)
                        .reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTournament(UUID tournamentId, UUID organizerId) {
        Tournament tournament = getOwnedTournament(tournamentId, organizerId);

        if (tournament.getStatus() != TournamentStatus.PLANNED) {
            throw new IllegalArgumentException(
                    "Only tournaments that haven't started yet can be deleted. Once a bracket is generated, results are kept for the record."
            );
        }

        List<Match> matches = matchRepo.findByTournament_IdOrderByRoundOrderAsc(tournamentId);
        matchRepo.deleteAll(matches);

        List<Team> teams = teamRepo.findByTournament_Id(tournamentId);
        teamRepo.deleteAll(teams);

        tournamentRepo.delete(tournament);
    }

    @Override
    public List<TournamentResponseDto> getMyTournaments(UUID organizerId) {
        return tournamentRepo.findByOrganizerId(organizerId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ---------------- helpers ----------------

    private Tournament getOwnedTournament(UUID tournamentId, UUID organizerId) {
        Tournament tournament = tournamentRepo.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found"));
        if (!tournament.getBooking().getUser().getUserid().equals(organizerId)) {
            throw new IllegalArgumentException("You are not the organizer of this tournament");
        }
        return tournament;
    }

    private TournamentResponseDto toDto(Tournament t) {
        return new TournamentResponseDto(
                t.getId(),
                t.getBooking().getId(),
                t.getBooking().getListing().getTitle(),
                t.getBooking().getUser().getUserid(),
                t.getBooking().getUser().getName(),
                t.getName(),
                t.getSportType(),
                t.getFormat(),
                t.getMaxTeams(),
                t.getStatus(),
                t.getCreatedAt()
        );
    }

    private TeamResponseDto toTeamDto(Team team) {
        return new TeamResponseDto(
                team.getId(),
                team.getTournament().getId(),
                team.getName(),
                team.getRegisteredBy() != null ? team.getRegisteredBy().getUserid() : null
        );
    }

    private MatchResponseDto toMatchDto(Match m) {
        return new MatchResponseDto(
                m.getId(),
                m.getTournament().getId(),
                m.getTeamA() != null ? m.getTeamA().getId() : null,
                m.getTeamA() != null ? m.getTeamA().getName() : "TBD",
                m.getTeamB() != null ? m.getTeamB().getId() : null,
                m.getTeamB() != null ? m.getTeamB().getName() : "TBD",
                m.getScoreA(),
                m.getScoreB(),
                m.getRound(),
                m.getRoundOrder(),
                m.getBracketSide(),
                m.getStatus(),
                m.getScheduledAt()
        );
    }
}