package com.zorvyn.financedashboard.controller;

import com.zorvyn.financedashboard.dto.response.ApiResponse;
import com.zorvyn.financedashboard.dto.response.InsightResponse;
import com.zorvyn.financedashboard.service.InsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
@Tag(name = "Insights", description = "AI-driven spending analytics and anomaly detection")
public class InsightController {

    private final InsightService insightService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
            summary = "Get financial insights and anomaly detection",
            description = "Returns the highest spending category, average monthly expenditure, " +
                    "month-over-month spending change, and flags spending anomalies " +
                    "when current month exceeds 150% of the 6-month trailing average. " +
                    "Only ADMIN and ANALYST roles can access analytics."
    )
    public ResponseEntity<ApiResponse<InsightResponse>> getInsights() {
        InsightResponse insights = insightService.getInsights();
        return ResponseEntity.ok(ApiResponse.success("Insights retrieved successfully", insights));
    }
}
