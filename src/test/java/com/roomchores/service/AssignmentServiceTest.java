package com.roomchores.service;

import com.roomchores.model.Chore;
import com.roomchores.model.Frequency;
import com.roomchores.model.Roommate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentServiceTest {

    private static Chore chore(Frequency frequency, String skill) {
        Chore chore = new Chore();
        chore.code = "TEST";
        chore.name = "Test chore";
        chore.category = "Test";
        chore.points = 3;
        chore.frequency = frequency;
        chore.requiredSkill = skill;
        return chore;
    }

    private static Roommate roommate(String... skills) {
        Roommate roommate = new Roommate();
        roommate.name = "Tester";
        roommate.skills.addAll(java.util.List.of(skills));
        return roommate;
    }

    @Test
    void choreWithoutSkillIsOpenToEveryone() {
        assertTrue(AssignmentService.isEligible(roommate(), chore(Frequency.DAILY, null)));
    }

    @Test
    void cookingIsRestrictedToPeopleWhoCook() {
        Chore cooking = chore(Frequency.DAILY, "COOKING");
        assertFalse(AssignmentService.isEligible(roommate(), cooking));
        assertTrue(AssignmentService.isEligible(roommate("COOKING"), cooking));
    }

    @Test
    void cooksCanDoBasicPrepButNotTheOtherWayRound() {
        assertTrue(AssignmentService.isEligible(roommate("COOKING"), chore(Frequency.DAILY, "BASIC_PREP")));
        assertFalse(AssignmentService.isEligible(roommate("BASIC_PREP"), chore(Frequency.DAILY, "COOKING")));
    }

    @Test
    void inactiveRoommatesAreNeverEligible() {
        Roommate inactive = roommate();
        inactive.active = false;
        assertFalse(AssignmentService.isEligible(inactive, chore(Frequency.DAILY, null)));
    }

    @Test
    void recurrenceRules() {
        LocalDate monday = LocalDate.of(2026, 10, 12);
        LocalDate firstOfMonth = LocalDate.of(2026, 10, 1);
        LocalDate tuesday = LocalDate.of(2026, 10, 13);

        assertTrue(AssignmentService.isDue(chore(Frequency.DAILY, null), tuesday));
        assertTrue(AssignmentService.isDue(chore(Frequency.WEEKLY, null), monday));
        assertFalse(AssignmentService.isDue(chore(Frequency.WEEKLY, null), tuesday));
        assertTrue(AssignmentService.isDue(chore(Frequency.MONTHLY, null), firstOfMonth));
        assertFalse(AssignmentService.isDue(chore(Frequency.MONTHLY, null), tuesday));
        assertFalse(AssignmentService.isDue(chore(Frequency.AS_NEEDED, null), monday));
    }
}
