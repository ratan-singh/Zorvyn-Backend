package com.zorvyn.financedashboard.controller;

import com.zorvyn.financedashboard.dto.request.TransactionRequest;
import com.zorvyn.financedashboard.dto.response.ApiResponse;
import com.zorvyn.financedashboard.dto.response.TransactionResponse;
import com.zorvyn.financedashboard.model.enums.TransactionType;
import com.zorvyn.financedashboard.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Create, read, update, and soft-delete financial transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
            summary = "Create a new transaction",
            description = "Creates a new financial transaction. Only ADMIN and ANALYST roles can create transactions."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Transaction created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    @Operation(
            summary = "List all transactions (paginated, filterable)",
            description = "Returns all non-deleted transactions with optional filters. " +
                    "Supports pagination and sorting. All authenticated roles can view transactions."
    )
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAllTransactions(
            @Parameter(description = "Filter by transaction type")
            @RequestParam(required = false) TransactionType type,

            @Parameter(description = "Filter by category name")
            @RequestParam(required = false) String category,

            @Parameter(description = "Filter by start date (inclusive)")
            @RequestParam(required = false) LocalDate startDate,

            @Parameter(description = "Filter by end date (inclusive)")
            @RequestParam(required = false) LocalDate endDate,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "transactionDate") String sortBy,

            @Parameter(description = "Sort direction (ASC or DESC)")
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TransactionResponse> transactions =
                transactionService.getAllTransactions(type, category, startDate, endDate, pageable);

        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved successfully", transactions));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    @Operation(
            summary = "Get a single transaction by ID",
            description = "Returns a transaction by its UUID. Only non-deleted transactions are returned."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable UUID id) {
        TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
            summary = "Update an existing transaction",
            description = "Updates a transaction. ADMIN can update any transaction. " +
                    "ANALYST can only update transactions they created."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the owner and not ADMIN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.updateTransaction(id, request);
        return ResponseEntity.ok(ApiResponse.success("Transaction updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
            summary = "Soft-delete a transaction",
            description = "Marks a transaction as deleted (soft-delete). " +
                    "ADMIN can delete any transaction. ANALYST can only delete transactions they created."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not the owner and not ADMIN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @PathVariable UUID id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully", null));
    }
}
