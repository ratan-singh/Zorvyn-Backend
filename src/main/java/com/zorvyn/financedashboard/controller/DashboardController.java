package com.zorvyn.financedashboard.controller;

import com.zorvyn.financedashboard.dto.response.ApiResponse;
import com.zorvyn.financedashboard.dto.response.CategoryBreakdownResponse;
import com.zorvyn.financedashboard.dto.response.DashboardSummaryResponse;
import com.zorvyn.financedashboard.dto.response.MonthlyTrendResponse;
import com.zorvyn.financedashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregated financial overview — summary, trends, and category breakdown")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    @Operation(
            summary = "Get dashboard summary",
            description = "Returns total income, total expenses, net balance, and transaction counts across all non-deleted transactions."
    )
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary() {
        DashboardSummaryResponse summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary retrieved successfully", summary));
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    @Operation(
            summary = "Get monthly income/expense trends",
            description = "Returns income and expense totals grouped by month for the last 12 months, ordered chronologically."
    )
    public ResponseEntity<ApiResponse<List<MonthlyTrendResponse>>> getMonthlyTrends() {
        List<MonthlyTrendResponse> trends = dashboardService.getMonthlyTrends();
        return ResponseEntity.ok(ApiResponse.success("Monthly trends retrieved successfully", trends));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    @Operation(
            summary = "Get expense category breakdown",
            description = "Returns expense amounts grouped by category with percentage of total spend and transaction count."
    )
    public ResponseEntity<ApiResponse<List<CategoryBreakdownResponse>>> getCategoryBreakdown() {
        List<CategoryBreakdownResponse> breakdown = dashboardService.getCategoryBreakdown();
        return ResponseEntity.ok(ApiResponse.success("Category breakdown retrieved successfully", breakdown));
    }
}
