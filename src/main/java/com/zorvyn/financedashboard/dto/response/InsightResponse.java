package com.zorvyn.financedashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightResponse {

    private String highestSpendingCategory;
    private BigDecimal averageMonthlyExpenditure;

    /**
     * Percentage change: ((currentMonth - previousMonth) / previousMonth) * 100
     * Null when the previous month has no expense data — avoids division by zero.
     */
    private BigDecimal currentMonthVsPreviousMonthChange;

    private boolean spendingAnomalyDetected;

    /**
     * Human-readable anomaly description, e.g.:
     * "Spending in March 2026 was 180% above your 6-month average"
     * Null when no anomaly exists.
     */
    private String anomalyDescription;
}
