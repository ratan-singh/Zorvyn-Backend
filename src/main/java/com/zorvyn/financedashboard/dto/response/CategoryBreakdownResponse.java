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
public class CategoryBreakdownResponse {

    private String category;
    private BigDecimal totalAmount;
    private BigDecimal percentage;
    private long transactionCount;
}
