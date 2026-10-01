package com.bca.rentora.rentora.services.implementation;

import com.bca.rentora.rentora.dtos.Tournament.*;
import com.bca.rentora.rentora.entity.*;
import com.bca.rentora.rentora.exceptions.ResourceNotFoundException;
import com.bca.rentora.rentora.repo.BookingRepo;
import com.bca.rentora.rentora.repo.MatchRepo;
import com.bca.rentora.rentora.repo.TeamRepo;
import com.bca.rentora.rentora.repo.TournamentRepo;
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

    public TournamentServiceImpl(TournamentRepo tournamentRepo, TeamRepo teamRepo,
                                 MatchRepo matchRepo, BookingRepo bookingRepo) {
        this.tournamentRepo = tournamentRepo;
        this.teamRepo = teamRepo;
        this.matchRepo = matchRepo;
        this.bookingRepo = bookingRepo;
    }



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



    @Override
    @Transactional
    public TeamResponseDto addTeam(UUID tournamentId, TeamCreateDto dto, UUID organizerId) {
        Tournament tournament = getOwnedTournament(tournamentId, organizerId);

        if (tournament.getStatus() != TournamentStatus.PLANNED) {
            throw new IllegalArgumentException("Teams can only be added while the tournament is still being planned");
        }

        long currentCount = teamRepo.findByTournament_Id(tournamentId).size();
        if (currentCount >= tournament.getMaxTeams()) {
            throw new IllegalArgumentException("Maximum number of teams already reached");
        }

        Team team = new Team();
        team.setTournament(tournament);
        team.setName(dto.name());
        teamRepo.save(team);

        return toTeamDto(team);
    }

    @Override
    public List<TeamResponseDto> getTeams(UUID tournamentId) {
        return teamRepo.findByTournament_Id(tournamentId)
                .stream()
                .map(this::toTeamDto)
                .collect(Collectors.toList());
    }



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

        List<Match> matches = switch (tournament.getFormat()) {
            case KNOCKOUT -> generateKnockout(tournament, teams);
            case DOUBLE_ELIMINATION -> generateDoubleElimination(tournament, teams);
            case ROUND_ROBIN, LEAGUE -> generateRoundRobin(tournament, teams);
        };

        matchRepo.saveAll(matches);

        tournament.setStatus(TournamentStatus.ONGOING);
        tournamentRepo.save(tournament);

        return matches.stream().map(this::toMatchDto).collect(Collectors.toList());
    }

    // ---------------- Bracket generation (structural-pointer based) ----------------

    private List<Match> generateKnockout(Tournament tournament, List<Team> teams) {
        List<Team> shuffled = new ArrayList<>(teams);
        Collections.shuffle(shuffled);

        int size = 1;
        while (size < shuffled.size()) size *= 2;
        while (shuffled.size() < size) shuffled.add(null); // null = bye

        List<List<Match>> rounds = new ArrayList<>();

        // Round 1
        List<Match> round1 = new ArrayList<>();
        for (int i = 0; i < shuffled.size(); i += 2) {
            Match m = new Match();
            m.setTournament(tournament);
            m.setTeamA(shuffled.get(i));
            m.setTeamB(shuffled.get(i + 1));
            m.setRound("Round 1");
            m.setRoundOrder(round1.size());
            m.setStatus(MatchStatus.SCHEDULED);
            round1.add(m);
        }
        rounds.add(round1);

        // Subsequent rounds — empty placeholders, wired below
        int remaining = size / 2;
        int roundNum = 2;
        while (remaining > 1) {
            List<Match> round = new ArrayList<>();
            String roundName = remaining == 2 ? "Final" : (remaining == 4 ? "Semifinal" : "Round " + roundNum);
            for (int i = 0; i < remaining / 2; i++) {
                Match m = new Match();
                m.setTournament(tournament);
                m.setRound(roundName);
                m.setRoundOrder(i);
                m.setStatus(MatchStatus.SCHEDULED);
                round.add(m);
            }
            rounds.add(round);
            remaining /= 2;
            roundNum++;
        }

        wireSequentialRounds(rounds);

        List<Match> allMatches = rounds.stream().flatMap(List::stream).collect(Collectors.toList());

        // Resolve byes immediately using the now-explicit pointers
        for (Match m : round1) {
            if (m.getTeamA() == null || m.getTeamB() == null) {
                Team winner = m.getTeamA() != null ? m.getTeamA() : m.getTeamB();
                m.setStatus(MatchStatus.FORFEIT);
                if (winner != null) {
                    advanceWinner(m, winner);
                }
            }
        }

        return allMatches;
    }

    /**
     * Wires each match in round r to the match it feeds into in round r+1.
     * Match i and i+1 in round r both point to match (i/2) in round r+1 —
     * i even -> slot 0 (teamA), i odd -> slot 1 (teamB).
     */
    private void wireSequentialRounds(List<List<Match>> rounds) {
        for (int r = 0; r < rounds.size() - 1; r++) {
            List<Match> current = rounds.get(r);
            List<Match> next = rounds.get(r + 1);
            for (int i = 0; i < current.size(); i++) {
                Match m = current.get(i);
                Match target = next.get(i / 2);
                m.setNextMatch(target);
                m.setNextMatchSlot(i % 2);
            }
        }
    }

    private List<Match> generateRoundRobin(Tournament tournament, List<Team> teams) {
        List<Match> matches = new ArrayList<>();
        int order = 0;
        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                Match m = new Match();
                m.setTournament(tournament);
                m.setTeamA(teams.get(i));
                m.setTeamB(teams.get(j));
                m.setRound("Round Robin");
                m.setRoundOrder(order++);
                m.setStatus(MatchStatus.SCHEDULED);
                matches.add(m);
            }
        }
        return matches;
    }

    private List<Match> generateDoubleElimination(Tournament tournament, List<Team> teams) {
        // Winners bracket: same pointer-based structure as knockout.
        // Losers bracket: generated as placeholder matches (no auto-seeding of
        // drops from the winners bracket — this remains a documented
        // simplification; organizer manages losers-bracket pairings manually
        // via score entry once teams are known).
        List<Team> shuffled = new ArrayList<>(teams);
        Collections.shuffle(shuffled);

        int size = 1;
        while (size < shuffled.size()) size *= 2;
        while (shuffled.size() < size) shuffled.add(null);

        List<List<Match>> winnersRounds = new ArrayList<>();

        List<Match> wRound1 = new ArrayList<>();
        for (int i = 0; i < shuffled.size(); i += 2) {
            Match m = new Match();
            m.setTournament(tournament);
            m.setTeamA(shuffled.get(i));
            m.setTeamB(shuffled.get(i + 1));
            m.setRound("Winners Round 1");
            m.setRoundOrder(wRound1.size());
            m.setBracketSide(BracketSide.WINNERS);
            m.setStatus(MatchStatus.SCHEDULED);
            wRound1.add(m);
        }
        winnersRounds.add(wRound1);

        int remaining = size / 2;
        int roundNum = 2;
        while (remaining > 1) {
            List<Match> round = new ArrayList<>();
            String roundName = remaining == 2 ? "Winners Final" : "Winners Round " + roundNum;
            for (int i = 0; i < remaining / 2; i++) {
                Match m = new Match();
                m.setTournament(tournament);
                m.setRound(roundName);
                m.setRoundOrder(i);
                m.setBracketSide(BracketSide.WINNERS);
                m.setStatus(MatchStatus.SCHEDULED);
                round.add(m);
            }
            winnersRounds.add(round);
            remaining /= 2;
            roundNum++;
        }

        wireSequentialRounds(winnersRounds);

        List<Match> allMatches = new ArrayList<>(
                winnersRounds.stream().flatMap(List::stream).collect(Collectors.toList())
        );

        // Losers bracket — placeholders, not auto-wired
        int losersRounds = Math.max((int) (Math.log(size) / Math.log(2)) - 1, 1);
        for (int r = 1; r <= losersRounds; r++) {
            int matchesInRound = Math.max(size / 4, 1);
            for (int i = 0; i < matchesInRound; i++) {
                Match m = new Match();
                m.setTournament(tournament);
                m.setRound("Losers Round " + r);
                m.setRoundOrder(i);
                m.setBracketSide(BracketSide.LOSERS);
                m.setStatus(MatchStatus.SCHEDULED);
                allMatches.add(m);
            }
        }

        // Grand final — winners bracket final feeds into it as teamA
        Match grandFinal = new Match();
        grandFinal.setTournament(tournament);
        grandFinal.setRound("Grand Final");
        grandFinal.setRoundOrder(0);
        grandFinal.setBracketSide(BracketSide.GRAND_FINAL);
        grandFinal.setStatus(MatchStatus.SCHEDULED);
        allMatches.add(grandFinal);

        Match winnersFinal = winnersRounds.get(winnersRounds.size() - 1).get(0);
        winnersFinal.setNextMatch(grandFinal);
        winnersFinal.setNextMatchSlot(0);

        // Resolve round-1 byes in the winners bracket
        for (Match m : wRound1) {
            if (m.getTeamA() == null || m.getTeamB() == null) {
                Team winner = m.getTeamA() != null ? m.getTeamA() : m.getTeamB();
                m.setStatus(MatchStatus.FORFEIT);
                if (winner != null) {
                    advanceWinner(m, winner);
                }
            }
        }

        return allMatches;
    }

    private void advanceWinner(Match completedMatch, Team winner) {
        Match next = completedMatch.getNextMatch();
        if (next == null) return; // final match — nobody to advance to

        if (completedMatch.getNextMatchSlot() != null && completedMatch.getNextMatchSlot() == 0) {
            next.setTeamA(winner);
        } else {
            next.setTeamB(winner);
        }
    }



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

        match.setScoreA(dto.scoreA());
        match.setScoreB(dto.scoreB());
        match.setStatus(MatchStatus.FINISHED);
        matchRepo.save(match);

        // For elimination formats, advance the winner via the explicit pointer
        if (tournament.getFormat() == TournamentFormat.KNOCKOUT
                || tournament.getFormat() == TournamentFormat.DOUBLE_ELIMINATION) {
            Team winner = dto.scoreA() > dto.scoreB() ? match.getTeamA() : match.getTeamB();
            advanceWinner(match, winner);
            if (match.getNextMatch() != null) {
                matchRepo.save(match.getNextMatch());
            }

            List<Match> allMatches = matchRepo.findByTournament_IdOrderByRoundOrderAsc(tournament.getId());
            boolean allFinished = allMatches.stream()
                    .allMatch(m -> m.getStatus() == MatchStatus.FINISHED || m.getStatus() == MatchStatus.FORFEIT);
            if (allFinished) {
                tournament.setStatus(TournamentStatus.COMPLETED);
                tournamentRepo.save(tournament);
            }
        } else {
            // round robin / league — check if all matches are done
            List<Match> allMatches = matchRepo.findByTournament_IdOrderByRoundOrderAsc(tournament.getId());
            boolean allFinished = allMatches.stream().allMatch(m -> m.getStatus() == MatchStatus.FINISHED);
            if (allFinished) {
                tournament.setStatus(TournamentStatus.COMPLETED);
                tournamentRepo.save(tournament);
            }
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

        match.setScheduledAt(dto.scheduledAt());
        matchRepo.save(match);

        return toMatchDto(match);
    }



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

            a[0]++; b[0]++; // played
            if (m.getScoreA() > m.getScoreB()) {
                a[1]++; a[4] += 3; // win = 3 pts
                b[2]++;
            } else if (m.getScoreB() > m.getScoreA()) {
                b[1]++; b[4] += 3;
                a[2]++;
            } else {
                a[3]++; a[4] += 1; // draw = 1 pt each
                b[3]++; b[4] += 1;
            }
        }

        return teams.stream()
                .map(t -> {
                    int[] s = stats.get(t.getId());
                    return new StandingRowDto(t.getId(), t.getName(), s[0], s[1], s[2], s[3], s[4]);
                })
                .sorted(Comparator.comparingInt(StandingRowDto::points).reversed())
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
        return new TeamResponseDto(team.getId(), team.getTournament().getId(), team.getName());
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