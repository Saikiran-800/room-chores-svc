package com.roomchores.service;

import com.roomchores.model.Assignment;
import com.roomchores.model.AssignmentStatus;
import com.roomchores.model.Chore;
import com.roomchores.model.Roommate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class AssignmentService {

    /** A roommate who can COOK is automatically fine for tasks that only need BASIC_PREP. */
    private static final Map<String, Set<String>> IMPLIED_BY = Map.of("BASIC_PREP", Set.of("COOKING"));

    @ConfigProperty(name = "roomchores.zone", defaultValue = "Asia/Kolkata")
    String zone;

    public record GenerateResult(LocalDate date, List<Assignment> created, List<String> skipped) {
    }

    public record RoommateFairness(UUID roommateId, String name, int earned, int assigned, int missed,
                                   double target, double delta) {
    }

    public record FairnessReport(String month, int totalWorkload, List<RoommateFairness> roommates) {
    }

    public LocalDate today() {
        return LocalDate.now(ZoneId.of(zone));
    }

    public static boolean isEligible(Roommate roommate, Chore chore) {
        if (!roommate.active) {
            return false;
        }
        String skill = chore.requiredSkill;
        if (skill == null || skill.isBlank()) {
            return true;
        }
        if (roommate.skills.contains(skill)) {
            return true;
        }
        return IMPLIED_BY.getOrDefault(skill, Set.of()).stream().anyMatch(roommate.skills::contains);
    }

    /** Daily chores every day, weekly chores on Mondays, monthly chores on the 1st. */
    public static boolean isDue(Chore chore, LocalDate date) {
        return switch (chore.frequency) {
            case DAILY -> true;
            case WEEKLY -> date.getDayOfWeek() == DayOfWeek.MONDAY;
            case MONTHLY -> date.getDayOfMonth() == 1;
            case AS_NEEDED -> false;
        };
    }

    /**
     * Greedy balancing: heaviest chores first, each given to the eligible roommate with the
     * lowest (points already carried this month / capacity). Ties go to whoever did this
     * chore least in the last 7 days, so unpleasant jobs rotate.
     */
    @Transactional
    public GenerateResult generate(LocalDate date) {
        List<Roommate> activeRoommates = Roommate.list("active = true");
        List<Roommate> roommates = activeRoommates.stream().filter(r -> r.capacityPercent > 0).toList();
        if (roommates.isEmpty()) {
            throw new BadRequestException("Add at least one active roommate with capacity above 0 first");
        }

        YearMonth month = YearMonth.from(date);
        Map<UUID, Double> load = new HashMap<>();
        for (Roommate r : roommates) {
            load.put(r.id, 0.0);
        }

        List<Assignment> monthAssignments = Assignment.list(
                "dueDate >= ?1 and dueDate <= ?2 and status <> ?3",
                month.atDay(1), month.atEndOfMonth(), AssignmentStatus.MISSED);
        for (Assignment a : monthAssignments) {
            load.computeIfPresent(a.roommate.id, (k, v) -> v + a.chore.points);
        }

        Set<String> alreadyAssigned = new HashSet<>();
        List<Assignment> sameDay = Assignment.list("dueDate", date);
        for (Assignment a : sameDay) {
            alreadyAssigned.add(a.chore.code);
        }

        Map<String, Map<UUID, Integer>> recent = new HashMap<>();
        List<Assignment> lastWeek = Assignment.list("dueDate >= ?1 and dueDate < ?2", date.minusDays(7), date);
        for (Assignment a : lastWeek) {
            recent.computeIfAbsent(a.chore.code, k -> new HashMap<>()).merge(a.roommate.id, 1, Integer::sum);
        }

        List<Chore> enabledChores = Chore.list("enabled = true");
        List<Chore> due = enabledChores.stream()
                .filter(c -> isDue(c, date))
                .filter(c -> !alreadyAssigned.contains(c.code))
                .sorted(Comparator.comparingInt((Chore c) -> -c.points).thenComparing(c -> c.code))
                .toList();

        List<Assignment> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (Chore chore : due) {
            Map<UUID, Integer> recentForChore = recent.getOrDefault(chore.code, Map.of());
            Roommate best = roommates.stream()
                    .filter(r -> isEligible(r, chore))
                    .min(Comparator.comparingDouble((Roommate r) -> load.get(r.id) / r.capacityPercent)
                            .thenComparingInt(r -> recentForChore.getOrDefault(r.id, 0))
                            .thenComparing(r -> r.name))
                    .orElse(null);
            if (best == null) {
                skipped.add(chore.code + ": no eligible roommate");
                continue;
            }
            Assignment assignment = new Assignment();
            assignment.chore = chore;
            assignment.roommate = best;
            assignment.dueDate = date;
            assignment.persist();
            load.merge(best.id, (double) chore.points, Double::sum);
            created.add(assignment);
        }
        return new GenerateResult(date, created, skipped);
    }

    /**
     * Fair share = total workload that existed in the month x (roommate capacity / total capacity).
     * Missed chores still count as workload, so skipping them lowers your delta.
     */
    public FairnessReport fairness(YearMonth month) {
        List<Assignment> all = Assignment.list("dueDate >= ?1 and dueDate <= ?2",
                month.atDay(1), month.atEndOfMonth());
        List<Roommate> active = Roommate.list("active = true");

        int totalWorkload = all.stream().mapToInt(a -> a.chore.points).sum();
        int totalCapacity = active.stream().mapToInt(r -> r.capacityPercent).sum();

        Map<UUID, Roommate> people = new LinkedHashMap<>();
        for (Roommate r : active) {
            people.put(r.id, r);
        }
        for (Assignment a : all) {
            people.putIfAbsent(a.roommate.id, a.roommate);
        }

        List<RoommateFairness> rows = new ArrayList<>();
        for (Roommate r : people.values()) {
            int earned = 0;
            int assigned = 0;
            int missed = 0;
            for (Assignment a : all) {
                if (!a.roommate.id.equals(r.id)) {
                    continue;
                }
                if (a.status == AssignmentStatus.COMPLETED) {
                    earned += a.pointsAwarded;
                }
                if (a.status == AssignmentStatus.MISSED) {
                    missed++;
                } else {
                    assigned += a.chore.points;
                }
            }
            double target = (r.active && totalCapacity > 0)
                    ? (double) totalWorkload * r.capacityPercent / totalCapacity
                    : 0.0;
            rows.add(new RoommateFairness(r.id, r.name, earned, assigned, missed,
                    round1(target), round1(earned - target)));
        }
        rows.sort(Comparator.comparing(RoommateFairness::name));
        return new FairnessReport(month.toString(), totalWorkload, rows);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
