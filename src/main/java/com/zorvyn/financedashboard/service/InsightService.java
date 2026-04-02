package com.zorvyn.financedashboard.service;

import com.zorvyn.financedashboard.dto.response.InsightResponse;
import com.zorvyn.financedashboard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightService {

    private static final BigDecimal ANOMALY_THRESHOLD_PERCENT = BigDecimal.valueOf(150);
    private static final int ANOMALY_LOOKBACK_MONTHS = 6;

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public InsightResponse getInsights() {
        String topCategory = transactionRepository.findHighestSpendingCategory();

        BigDecimal avgMonthlyExpenditure = calculateAverageMonthlyExpenditure();
        BigDecimal momChange = calculateMonthOverMonthChange();

        boolean anomalyDetected = false;
        String anomalyDescription = null;

        // Anomaly detection: compare current month spending to the 6-month trailing average
        BigDecimal sixMonthAvg = calculateTrailingAverage(ANOMALY_LOOKBACK_MONTHS);
        if (sixMonthAvg != null && sixMonthAvg.compareTo(BigDecimal.ZERO) > 0) {
            YearMonth currentMonth = YearMonth.now();
            BigDecimal currentMonthTotal = transactionRepository.fetchExpenseTotalForMonth(
                    currentMonth.getYear(), currentMonth.getMonthValue());

            if (currentMonthTotal != null && currentMonthTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal percentageOfAvg = currentMonthTotal
                        .multiply(BigDecimal.valueOf(100))
                        .divide(sixMonthAvg, 2, RoundingMode.HALF_UP);

                if (percentageOfAvg.compareTo(ANOMALY_THRESHOLD_PERCENT) > 0) {
                    anomalyDetected = true;
                    anomalyDescription = String.format(
                            "Spending in %s %d was %.0f%% above your %d-month average",
                            currentMonth.getMonth().name().substring(0, 1)
                                    + currentMonth.getMonth().name().substring(1).toLowerCase(),
                            currentMonth.getYear(),
                            percentageOfAvg.subtract(BigDecimal.valueOf(100)),
                            ANOMALY_LOOKBACK_MONTHS);
                }
            }
        }

        return InsightResponse.builder()
                .highestSpendingCategory(topCategory != null ? topCategory : "N/A")
                .averageMonthlyExpenditure(avgMonthlyExpenditure)
                .currentMonthVsPreviousMonthChange(momChange)
                .spendingAnomalyDetected(anomalyDetected)
                .anomalyDescription(anomalyDescription)
                .build();
    }

    /**
     * Average monthly expenditure = total all-time expenses / distinct expense months
     */
    private BigDecimal calculateAverageMonthlyExpenditure() {
        BigDecimal totalExpenses = transactionRepository.fetchTotalAllTimeExpenses();
        Long distinctMonths = transactionRepository.countDistinctExpenseMonths();

        if (totalExpenses == null || distinctMonths == null || distinctMonths == 0) {
            return BigDecimal.ZERO;
        }

        return totalExpenses.divide(BigDecimal.valueOf(distinctMonths), 2, RoundingMode.HALF_UP);
    }

    /**
     * Month-over-month change: ((current - previous) / previous) * 100
     * Returns null if previous month has no data (avoids division by zero).
     */
    private BigDecimal calculateMonthOverMonthChange() {
        YearMonth currentYm = YearMonth.now();
        YearMonth previousYm = currentYm.minusMonths(1);

        BigDecimal currentTotal = transactionRepository.fetchExpenseTotalForMonth(
                currentYm.getYear(), currentYm.getMonthValue());
        BigDecimal previousTotal = transactionRepository.fetchExpenseTotalForMonth(
                previousYm.getYear(), previousYm.getMonthValue());

        if (previousTotal == null || previousTotal.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return currentTotal.subtract(previousTotal)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousTotal, 2, RoundingMode.HALF_UP);
    }

    /**
     * Trailing average: average monthly spending over the last N months (excluding current).
     */
    private BigDecimal calculateTrailingAverage(int months) {
        LocalDate startDate = LocalDate.now().minusMonths(months).withDayOfMonth(1);
        List<Object[]> monthlyTotals = transactionRepository.fetchMonthlyExpenseTotals(startDate);

        // Exclude the current month from trailing average
        YearMonth currentYm = YearMonth.now();
        List<BigDecimal> pastMonthTotals = monthlyTotals.stream()
                .filter(row -> {
                    int year = ((Number) row[0]).intValue();
                    int month = ((Number) row[1]).intValue();
                    return !(year == currentYm.getYear() && month == currentYm.getMonthValue());
                })
                .map(row -> {
                    Object val = row[2];
                    return val instanceof BigDecimal bd ? bd : new BigDecimal(val.toString());
                })
                .toList();

        if (pastMonthTotals.isEmpty()) {
            return null;
        }

        BigDecimal sum = pastMonthTotals.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(pastMonthTotals.size()), 2, RoundingMode.HALF_UP);
    }
}
