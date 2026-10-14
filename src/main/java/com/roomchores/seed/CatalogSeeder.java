package com.roomchores.seed;

import com.roomchores.model.Chore;
import com.roomchores.model.Frequency;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/**
 * Seeds a trimmed, bundle-based chore catalog the first time the app starts against an empty
 * database. Everything is editable afterwards through the API / UI, so this is only a starting point.
 */
@ApplicationScoped
public class CatalogSeeder {

    private static final Logger LOG = Logger.getLogger(CatalogSeeder.class);

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (Chore.count() > 0) {
            return;
        }

        // Cooking (skill-gated) — one bundle per meal so nobody farms points from micro-steps
        seed("PREP_VEG", "Wash and chop vegetables", "Cooking", 2, 15, Frequency.DAILY, "BASIC_PREP", "Kitchen");
        seed("COOK_BREAKFAST", "Make breakfast (upma / poha)", "Cooking", 3, 25, Frequency.DAILY, "COOKING", "Kitchen");
        seed("COOK_LUNCH", "Cook lunch (rice + curry + roti)", "Cooking", 7, 60, Frequency.DAILY, "COOKING", "Kitchen");
        seed("COOK_DINNER", "Cook dinner (rice + curry + roti)", "Cooking", 7, 60, Frequency.DAILY, "COOKING", "Kitchen");

        // Kitchen cleanup
        seed("CLEANUP_LUNCH", "Post-meal kitchen cleanup (lunch)", "Kitchen", 4, 25, Frequency.DAILY, null, "Kitchen");
        seed("CLEANUP_DINNER", "Post-meal kitchen cleanup (dinner)", "Kitchen", 4, 25, Frequency.DAILY, null, "Kitchen");
        seed("KITCHEN_FLOOR", "Sweep and mop kitchen floor", "Kitchen", 3, 15, Frequency.DAILY, null, "Kitchen");
        seed("KITCHEN_DEEP", "Deep-clean the kitchen", "Kitchen", 5, 40, Frequency.WEEKLY, null, "Kitchen");
        seed("MICROWAVE_CLEAN", "Clean microwave / air fryer", "Kitchen", 2, 10, Frequency.WEEKLY, null, "Kitchen");
        seed("FRIDGE_CLEAN", "Clean the refrigerator inside", "Kitchen", 4, 25, Frequency.MONTHLY, null, "Kitchen");
        seed("PANTRY_ORGANIZE", "Organize pantry and discard expired food", "Kitchen", 3, 20, Frequency.MONTHLY, null, "Kitchen");

        // Common area
        seed("SWEEP_MOP_ROOM", "Sweep and mop the shared room", "Common area", 4, 20, Frequency.DAILY, null, "Living Room");
        seed("DUST_SHELVES", "Dust shelves and surfaces", "Common area", 2, 10, Frequency.WEEKLY, null, "Living Room");
        seed("BALCONY_CLEAN", "Sweep and mop the balcony", "Common area", 3, 15, Frequency.WEEKLY, null, "Balcony");
        seed("WINDOWS_CLEAN", "Clean windows and window tracks", "Common area", 3, 20, Frequency.MONTHLY, null, "Entire Room");
        seed("FANS_DEEP", "Deep-clean fans (where safely reachable)", "Common area", 3, 20, Frequency.MONTHLY, null, "Entire Room");
        seed("ROOM_RESET", "Shared-room reset and organization", "Common area", 4, 30, Frequency.MONTHLY, null, "Entire Room");

        // Bathroom
        seed("BATH_CLEAN", "Clean the bathroom (basin, toilet, floor)", "Bathroom", 5, 35, Frequency.WEEKLY, null, "Bathroom");

        // Waste and water
        seed("BINS_OUT", "Empty bins and take garbage out", "Waste", 2, 10, Frequency.DAILY, null, "Kitchen");
        seed("WATER_REFILL", "Refill drinking water", "Utilities", 1, 5, Frequency.DAILY, null, "Kitchen");

        // Laundry (shared items only)
        seed("LAUNDRY_SHARED", "Shared laundry (towels, cloths, linens)", "Laundry", 3, 20, Frequency.WEEKLY, null, "Laundry Area");

        // Groceries and admin
        seed("GROCERY_WEEKLY", "Weekly grocery shopping", "Groceries", 3, 45, Frequency.WEEKLY, null, "Outside");
        seed("GROCERY_TOPUP", "Quick store run for a few items", "Groceries", 2, 20, Frequency.AS_NEEDED, null, "Outside");
        seed("SUPPLY_INVENTORY", "Inventory household supplies", "Groceries", 2, 15, Frequency.MONTHLY, null, "Entire Room");
        seed("SERVICE_COORDINATION", "Coordinate a repair / pest-control / plumber visit", "Admin", 2, 15, Frequency.AS_NEEDED, null, null);

        // Occasional
        seed("GUEST_SETUP", "Set up the room for guests", "Occasional", 3, 20, Frequency.AS_NEEDED, null, "Entire Room");
        seed("GUEST_CLEANUP", "Clean up after guests leave", "Occasional", 4, 30, Frequency.AS_NEEDED, null, "Entire Room");
        seed("NEW_ROOMMATE_PREP", "Prepare the room for a new roommate", "Occasional", 5, 45, Frequency.AS_NEEDED, null, "Entire Room");

        LOG.infof("Seeded %d chores into an empty catalog", Chore.count());
    }

    private static void seed(String code, String name, String category, int points, int minutes,
                             Frequency frequency, String requiredSkill, String location) {
        Chore chore = new Chore();
        chore.code = code;
        chore.name = name;
        chore.category = category;
        chore.points = points;
        chore.estimatedMinutes = minutes;
        chore.frequency = frequency;
        chore.requiredSkill = requiredSkill;
        chore.location = location;
        chore.enabled = true;
        chore.persist();
    }
}
