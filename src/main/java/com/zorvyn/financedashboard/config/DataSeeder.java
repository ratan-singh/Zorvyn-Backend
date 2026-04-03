package com.zorvyn.financedashboard.config;

import com.zorvyn.financedashboard.model.Transaction;
import com.zorvyn.financedashboard.model.User;
import com.zorvyn.financedashboard.model.enums.Role;
import com.zorvyn.financedashboard.model.enums.TransactionType;
import com.zorvyn.financedashboard.model.enums.UserStatus;
import com.zorvyn.financedashboard.repository.TransactionRepository;
import com.zorvyn.financedashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds the database with demo users and transactions on application startup.
 * Only runs when the "seed" profile is active:
 *   mvn spring-boot:run -Dspring-boot.run.profiles=seed
 *
 * Idempotent — skips seeding if any users already exist in the database.
 */
@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded — skipping.");
            return;
        }

        log.info("Seeding database with demo data...");

        // ─── Users ──────────────────────────────────────────────
        User admin = userRepository.save(User.builder()
                .name("Admin User")
                .email("admin@zorvyn.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());

        User analyst = userRepository.save(User.builder()
                .name("Analyst User")
                .email("analyst@zorvyn.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(Role.ANALYST)
                .status(UserStatus.ACTIVE)
                .build());

        User viewer = userRepository.save(User.builder()
                .name("Viewer User")
                .email("viewer@zorvyn.com")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .role(Role.VIEWER)
                .status(UserStatus.ACTIVE)
                .build());

        log.info("Seeded 3 users: admin, analyst, viewer");

        // ─── Transactions ───────────────────────────────────────
        LocalDate today = LocalDate.now();

        List<Transaction> transactions = List.of(
                // ── Current month income ─────────────────────────
                buildTxn(new BigDecimal("85000.00"), TransactionType.INCOME, "Salary",
                        "Monthly salary — April 2026", today.withDayOfMonth(1), admin),
                buildTxn(new BigDecimal("12000.00"), TransactionType.INCOME, "Freelance",
                        "UI/UX consulting project", today.withDayOfMonth(5), analyst),
                buildTxn(new BigDecimal("3500.00"), TransactionType.INCOME, "Investments",
                        "Dividend from mutual funds", today.withDayOfMonth(3), admin),

                // ── Current month expenses ───────────────────────
                buildTxn(new BigDecimal("25000.00"), TransactionType.EXPENSE, "Rent",
                        "Office space rent — Koramangala", today.withDayOfMonth(1), admin),
                buildTxn(new BigDecimal("8500.00"), TransactionType.EXPENSE, "Utilities",
                        "Electricity, internet, water", today.withDayOfMonth(4), analyst),
                buildTxn(new BigDecimal("4200.00"), TransactionType.EXPENSE, "Software",
                        "AWS hosting + GitHub Team plan", today.withDayOfMonth(2), admin),
                buildTxn(new BigDecimal("6800.00"), TransactionType.EXPENSE, "Marketing",
                        "Google Ads campaign — Q2 launch", today.withDayOfMonth(6), analyst),
                buildTxn(new BigDecimal("3200.00"), TransactionType.EXPENSE, "Food",
                        "Team lunch and snacks", today.withDayOfMonth(3), admin),

                // ── Previous month (March) ───────────────────────
                buildTxn(new BigDecimal("85000.00"), TransactionType.INCOME, "Salary",
                        "Monthly salary — March 2026", today.minusMonths(1).withDayOfMonth(1), admin),
                buildTxn(new BigDecimal("9000.00"), TransactionType.INCOME, "Freelance",
                        "Backend API project", today.minusMonths(1).withDayOfMonth(10), analyst),
                buildTxn(new BigDecimal("25000.00"), TransactionType.EXPENSE, "Rent",
                        "Office space rent — Koramangala", today.minusMonths(1).withDayOfMonth(1), admin),
                buildTxn(new BigDecimal("7200.00"), TransactionType.EXPENSE, "Utilities",
                        "Electricity, internet, water", today.minusMonths(1).withDayOfMonth(5), analyst),
                buildTxn(new BigDecimal("5500.00"), TransactionType.EXPENSE, "Software",
                        "AWS hosting + Figma subscription", today.minusMonths(1).withDayOfMonth(3), admin),
                buildTxn(new BigDecimal("2800.00"), TransactionType.EXPENSE, "Food",
                        "Team lunch and snacks", today.minusMonths(1).withDayOfMonth(15), admin),

                // ── Two months ago (February) ────────────────────
                buildTxn(new BigDecimal("85000.00"), TransactionType.INCOME, "Salary",
                        "Monthly salary — February 2026", today.minusMonths(2).withDayOfMonth(1), admin),
                buildTxn(new BigDecimal("25000.00"), TransactionType.EXPENSE, "Rent",
                        "Office space rent — Koramangala", today.minusMonths(2).withDayOfMonth(1), admin),
                buildTxn(new BigDecimal("6500.00"), TransactionType.EXPENSE, "Utilities",
                        "Electricity, internet, water", today.minusMonths(2).withDayOfMonth(4), analyst),
                buildTxn(new BigDecimal("15000.00"), TransactionType.EXPENSE, "Equipment",
                        "MacBook charger + monitor arm", today.minusMonths(2).withDayOfMonth(12), admin),

                // ── Three months ago (January) ───────────────────
                buildTxn(new BigDecimal("85000.00"), TransactionType.INCOME, "Salary",
                        "Monthly salary — January 2026", today.minusMonths(3).withDayOfMonth(1), admin),
                buildTxn(new BigDecimal("25000.00"), TransactionType.EXPENSE, "Rent",
                        "Office space rent — Koramangala", today.minusMonths(3).withDayOfMonth(1), admin),
                buildTxn(new BigDecimal("3800.00"), TransactionType.EXPENSE, "Software",
                        "AWS hosting + domain renewal", today.minusMonths(3).withDayOfMonth(3), admin)
        );

        transactionRepository.saveAll(transactions);
        log.info("Seeded {} transactions spanning 4 months", transactions.size());
        log.info("Database seeding complete.");
    }

    private Transaction buildTxn(BigDecimal amount, TransactionType type, String category,
                                  String description, LocalDate date, User createdBy) {
        return Transaction.builder()
                .amount(amount)
                .type(type)
                .category(category)
                .description(description)
                .transactionDate(date)
                .createdBy(createdBy)
                .build();
    }
}
