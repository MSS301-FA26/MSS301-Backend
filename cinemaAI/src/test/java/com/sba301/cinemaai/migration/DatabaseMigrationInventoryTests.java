package com.sba301.cinemaai.migration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabaseMigrationInventoryTests {

    private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");

    @Test
    void shouldContainRequiredMigrationVersions() {
        List<String> requiredFiles = List.of(
                "V1__baseline_schema.sql",
                "V2__seat_rows_layout.sql",
                "V3__actor_name_length.sql",
                "V4__movie_actor_main_role.sql",
                "V5__showtime_ticket_price_matrix.sql"
        );

        for (String file : requiredFiles) {
            assertTrue(Files.isRegularFile(MIGRATION_DIR.resolve(file)), "Missing migration file: " + file);
        }
    }

    @Test
    void baselineShouldDeclareCoreBusinessTablesAndIndexes() throws IOException {
        String baseline = readMigration("V1__baseline_schema.sql");

        List<String> requiredTables = List.of(
                "roles",
                "users",
                "user_profiles",
                "user_roles",
                "refresh_tokens",
                "password_reset_tokens",
                "email_verification_tokens",
                "pending_registrations",
                "phone_verification_tokens",
                "genres",
                "movies",
                "movie_genres",
                "actors",
                "movie_actors",
                "trailer_interactions",
                "user_preference_profiles",
                "user_cohort_preferences",
                "cinemas",
                "rooms",
                "seat_rows",
                "seats",
                "showtimes",
                "ticket_pricing_rules",
                "ticket_combos",
                "bookings",
                "booking_seats",
                "booking_tickets",
                "food_items",
                "food_combos",
                "booking_food_items",
                "payments",
                "wishlists",
                "loyalty_points",
                "notifications",
                "reviews",
                "staff_profiles",
                "staff_shifts",
                "audit_logs",
                "uploaded_files"
        );

        for (String table : requiredTables) {
            assertTrue(
                    baseline.contains("create table dbo." + table),
                    "Baseline migration must create table dbo." + table
            );
        }

        List<String> requiredIndexes = List.of(
                "idx_movies_status",
                "idx_showtimes_start_time",
                "idx_ticket_pricing_rules_lookup",
                "idx_bookings_status",
                "idx_booking_seats_showtime_seat_status",
                "uk_payments_transaction_id_not_null",
                "idx_notifications_user_read",
                "idx_reviews_status",
                "idx_audit_logs_target"
        );

        for (String index : requiredIndexes) {
            assertTrue(baseline.contains(index), "Baseline migration must create index " + index);
        }
    }

    @Test
    void incrementalMigrationsShouldCoverSeatLayoutActorAndTicketPriceMatrix() throws IOException {
        String seatLayout = readMigration("V2__seat_rows_layout.sql");
        assertTrue(seatLayout.contains("create table dbo.seat_rows"));
        assertTrue(seatLayout.contains("display_column"));
        assertTrue(seatLayout.contains("seat_row_id"));

        String actorNameLength = readMigration("V3__actor_name_length.sql");
        assertTrue(actorNameLength.contains("alter table dbo.actors alter column name nvarchar(50) not null"));
        assertTrue(actorNameLength.contains("uq_actors_name"));

        String movieActorMainRole = readMigration("V4__movie_actor_main_role.sql");
        assertTrue(movieActorMainRole.contains("is_main_actor"));

        String ticketPriceMatrix = readMigration("V5__showtime_ticket_price_matrix.sql");
        List<String> priceColumns = List.of(
                "adult_standard_price",
                "child_standard_price",
                "student_standard_price",
                "adult_vip_price",
                "child_vip_price",
                "student_vip_price",
                "adult_couple_price",
                "child_couple_price",
                "student_couple_price",
                "weekend_surcharge",
                "holiday_surcharge"
        );

        for (String column : priceColumns) {
            assertTrue(ticketPriceMatrix.contains(column), "V5 must include showtime price column " + column);
        }
    }

    private String readMigration(String fileName) throws IOException {
        return Files.readString(MIGRATION_DIR.resolve(fileName)).toLowerCase();
    }
}
