package com.zorvyn.financedashboard.repository;

import com.zorvyn.financedashboard.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>,
        JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findAllByIsDeletedFalse(Pageable pageable);

    // ─── Dashboard Summary ──────────────────────────────────────
    // Single query returning total income and total expenses to avoid two round-trips.
    // Returns Object[]{totalIncome, totalExpenses, incomeCount, expenseCount}.

    @Query("""
            SELECT
                COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN 1 ELSE 0 END), 0)
            FROM Transaction t
            WHERE t.isDeleted = false
            """)
    List<Object[]> fetchDashboardSummaryAggregates();

    // ─── Monthly Trends ─────────────────────────────────────────
    // Groups income and expense totals by year-month for the last 12 months.
    // Each row: [year, month, totalIncome, totalExpenses]

    @Query("""
            SELECT
                EXTRACT(YEAR FROM t.transactionDate) AS txnYear,
                EXTRACT(MONTH FROM t.transactionDate) AS txnMonth,
                COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' THEN t.amount ELSE 0 END), 0)
            FROM Transaction t
            WHERE t.isDeleted = false
              AND t.transactionDate >= :cutoffDate
            GROUP BY EXTRACT(YEAR FROM t.transactionDate), EXTRACT(MONTH FROM t.transactionDate)
            ORDER BY txnYear ASC, txnMonth ASC
            """)
    List<Object[]> fetchMonthlyTrendAggregates(@Param("cutoffDate") LocalDate cutoffDate);

    // ─── Category Breakdown ─────────────────────────────────────
    // Expense-only breakdown by category: [category, totalAmount, transactionCount]

    @Query("""
            SELECT
                t.category,
                SUM(t.amount),
                COUNT(t)
            FROM Transaction t
            WHERE t.isDeleted = false
              AND t.type = 'EXPENSE'
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
            """)
    List<Object[]> fetchExpenseCategoryBreakdown();

    // ─── Insight Queries ────────────────────────────────────────
    // Highest spending category across all time (non-deleted expenses only)

    @Query("""
            SELECT t.category
            FROM Transaction t
            WHERE t.isDeleted = false
              AND t.type = 'EXPENSE'
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
            LIMIT 1
            """)
    String findHighestSpendingCategory();

    // Monthly expense totals within a date range — used for anomaly detection and averages.
    // Each row: [year, month, totalExpense]

    @Query("""
            SELECT
                EXTRACT(YEAR FROM t.transactionDate),
                EXTRACT(MONTH FROM t.transactionDate),
                SUM(t.amount)
            FROM Transaction t
            WHERE t.isDeleted = false
              AND t.type = 'EXPENSE'
              AND t.transactionDate >= :startDate
            GROUP BY EXTRACT(YEAR FROM t.transactionDate), EXTRACT(MONTH FROM t.transactionDate)
            ORDER BY EXTRACT(YEAR FROM t.transactionDate) ASC, EXTRACT(MONTH FROM t.transactionDate) ASC
            """)
    List<Object[]> fetchMonthlyExpenseTotals(@Param("startDate") LocalDate startDate);

    // Total expenses across all time (non-deleted) — used for average monthly expenditure calculation

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.isDeleted = false
              AND t.type = 'EXPENSE'
            """)
    BigDecimal fetchTotalAllTimeExpenses();

    // Count of distinct months that have at least one expense transaction

    @Query("""
            SELECT COUNT(DISTINCT CONCAT(EXTRACT(YEAR FROM t.transactionDate), '-', EXTRACT(MONTH FROM t.transactionDate)))
            FROM Transaction t
            WHERE t.isDeleted = false
              AND t.type = 'EXPENSE'
            """)
    Long countDistinctExpenseMonths();

    // Current month total expenses

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.isDeleted = false
              AND t.type = 'EXPENSE'
              AND EXTRACT(YEAR FROM t.transactionDate) = :year
              AND EXTRACT(MONTH FROM t.transactionDate) = :month
            """)
    BigDecimal fetchExpenseTotalForMonth(@Param("year") int year, @Param("month") int month);
}
