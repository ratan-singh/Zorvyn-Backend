package com.zorvyn.financedashboard.service;

import com.zorvyn.financedashboard.dto.response.CategoryBreakdownResponse;
import com.zorvyn.financedashboard.dto.response.DashboardSummaryResponse;
import com.zorvyn.financedashboard.dto.response.MonthlyTrendResponse;
import com.zorvyn.financedashboard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        List<Object[]> results = transactionRepository.fetchDashboardSummaryAggregates();

        // Query always returns one row due to no GROUP BY, but guard against empty
        // result
        Object[] aggregates = results.isEmpty() ? new Object[] { null, null, null, null } : results.get(0);

        BigDecimal totalIncome = aggregates[0] != null ? toBigDecimal(aggregates[0]) : BigDecimal.ZERO;
        BigDecimal totalExpenses = aggregates[1] != null ? toBigDecimal(aggregates[1]) : BigDecimal.ZERO;
        long incomeCount = aggregates[2] != null ? toLong(aggregates[2]) : 0L;
        long expenseCount = aggregates[3] != null ? toLong(aggregates[3]) : 0L;

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(totalIncome.subtract(totalExpenses))
                .totalTransactionCount(incomeCount + expenseCount)
                .incomeTransactionCount(incomeCount)
                .expenseTransactionCount(expenseCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MonthlyTrendResponse> getMonthlyTrends() {
        LocalDate cutoffDate = LocalDate.now().minusMonths(12).withDayOfMonth(1);
        List<Object[]> rows = transactionRepository.fetchMonthlyTrendAggregates(cutoffDate);

        return rows.stream().map(row -> {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            BigDecimal income = toBigDecimal(row[2]);
            BigDecimal expenses = toBigDecimal(row[3]);

            return MonthlyTrendResponse.builder()
                    .yearMonth(String.format("%d-%02d", year, month))
                    .totalIncome(income)
                    .totalExpenses(expenses)
                    .netBalance(income.subtract(expenses))
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownResponse> getCategoryBreakdown() {
        List<Object[]> rows = transactionRepository.fetchExpenseCategoryBreakdown();

        // Calculate the total of all category amounts for percentage computation
        BigDecimal grandTotal = rows.stream()
                .map(row -> toBigDecimal(row[1]))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream().map(row -> {
            String category = (String) row[0];
            BigDecimal amount = toBigDecimal(row[1]);
            long count = toLong(row[2]);

            BigDecimal percentage = grandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? amount.multiply(BigDecimal.valueOf(100)).divide(grandTotal, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            return CategoryBreakdownResponse.builder()
                    .category(category)
                    .totalAmount(amount)
                    .percentage(percentage)
                    .transactionCount(count)
                    .build();
        }).toList();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null)
            return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd)
            return bd;
        return new BigDecimal(value.toString());
    }

    private long toLong(Object value) {
        if (value == null)
            return 0L;
        return ((Number) value).longValue();
    }
}
