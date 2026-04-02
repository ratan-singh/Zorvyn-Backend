package com.zorvyn.financedashboard.service;

import com.zorvyn.financedashboard.dto.request.TransactionRequest;
import com.zorvyn.financedashboard.dto.response.TransactionResponse;
import com.zorvyn.financedashboard.exception.ResourceNotFoundException;
import com.zorvyn.financedashboard.exception.UnauthorizedOperationException;
import com.zorvyn.financedashboard.model.Transaction;
import com.zorvyn.financedashboard.model.User;
import com.zorvyn.financedashboard.model.enums.Role;
import com.zorvyn.financedashboard.model.enums.TransactionType;
import com.zorvyn.financedashboard.repository.TransactionRepository;
import com.zorvyn.financedashboard.repository.TransactionSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    // ─── CREATE ─────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User currentUser = getAuthenticatedUser();

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .createdBy(currentUser)
                .build();

        Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);
        log.info("Transaction created: id={}, type={}, amount={}, by={}",
                savedTransaction.getId(), savedTransaction.getType(),
                savedTransaction.getAmount(), currentUser.getEmail());

        return mapToResponse(savedTransaction);
    }

    // ─── READ (all, paginated + filtered) ───────────────────────────

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(
            TransactionType type,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        return transactionRepository
                .findAll(TransactionSpecification.withFilters(type, category, startDate, endDate), pageable)
                .map(this::mapToResponse);
    }

    // ─── READ (single by ID) ────────────────────────────────────────

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID transactionId) {
        Transaction transaction = findActiveTransactionOrThrow(transactionId);
        return mapToResponse(transaction);
    }

    // ─── UPDATE ─────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse updateTransaction(UUID transactionId, TransactionRequest request) {
        Transaction transaction = findActiveTransactionOrThrow(transactionId);
        User currentUser = getAuthenticatedUser();

        enforceOwnershipOrAdmin(transaction, currentUser);

        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());

        Transaction updatedTransaction = transactionRepository.save(transaction);
        log.info("Transaction updated: id={}, by={}", transactionId, currentUser.getEmail());

        return mapToResponse(updatedTransaction);
    }

    // ─── SOFT DELETE ────────────────────────────────────────────────

    @Transactional
    public void deleteTransaction(UUID transactionId) {
        Transaction transaction = findActiveTransactionOrThrow(transactionId);
        User currentUser = getAuthenticatedUser();

        enforceOwnershipOrAdmin(transaction, currentUser);

        transaction.setIsDeleted(true);
        transactionRepository.save(transaction);
        log.info("Transaction soft-deleted: id={}, by={}", transactionId, currentUser.getEmail());
    }

    // ─── PRIVATE HELPERS ────────────────────────────────────────────

    /**
     * Finds a non-deleted transaction by ID.
     * Throws ResourceNotFoundException if the transaction doesn't exist or has been soft-deleted.
     */
    private Transaction findActiveTransactionOrThrow(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: " + transactionId));

        if (Boolean.TRUE.equals(transaction.getIsDeleted())) {
            throw new ResourceNotFoundException("Transaction not found with id: " + transactionId);
        }

        return transaction;
    }

    /**
     * Ensures the current user either owns the transaction or has ADMIN role.
     * ANALYST and VIEWER roles can only modify/delete their own transactions.
     */
    private void enforceOwnershipOrAdmin(Transaction transaction, User currentUser) {
        boolean isOwner = transaction.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedOperationException(
                    "You do not have permission to modify this transaction");
        }
    }

    /**
     * Extracts the authenticated User from the SecurityContext.
     * The principal is the User entity itself (implements UserDetails).
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    /**
     * Maps a Transaction entity to a TransactionResponse DTO.
     * Replaces the User entity reference with just the creator's name.
     */
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .category(transaction.getCategory())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .createdAt(transaction.getCreatedAt())
                .createdByName(transaction.getCreatedBy().getName())
                .build();
    }
}
