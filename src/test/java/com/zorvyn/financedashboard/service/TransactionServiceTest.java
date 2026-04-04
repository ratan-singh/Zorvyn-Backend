package com.zorvyn.financedashboard.service;

import com.zorvyn.financedashboard.dto.request.TransactionRequest;
import com.zorvyn.financedashboard.dto.response.TransactionResponse;
import com.zorvyn.financedashboard.exception.ResourceNotFoundException;
import com.zorvyn.financedashboard.model.Transaction;
import com.zorvyn.financedashboard.model.User;
import com.zorvyn.financedashboard.model.enums.Role;
import com.zorvyn.financedashboard.model.enums.TransactionType;
import com.zorvyn.financedashboard.model.enums.UserStatus;
import com.zorvyn.financedashboard.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ─── Helper methods ─────────────────────────────────────────────

    private void setAuthenticatedUser(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User buildAdminUser() {
        return User.builder()
                .id(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .name("Admin User")
                .email("admin@zorvyn.com")
                .passwordHash("$2a$12$hashedAdmin")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build();
    }

    private TransactionRequest buildTransactionRequest() {
        return TransactionRequest.builder()
                .amount(new BigDecimal("15000.50"))
                .type(TransactionType.INCOME)
                .category("Salary")
                .description("March salary")
                .transactionDate(LocalDate.of(2026, 3, 31))
                .build();
    }

    private Transaction buildTransaction(UUID id, User createdBy) {
        return Transaction.builder()
                .id(id)
                .amount(new BigDecimal("15000.50"))
                .type(TransactionType.INCOME)
                .category("Salary")
                .description("March salary")
                .transactionDate(LocalDate.of(2026, 3, 31))
                .createdBy(createdBy)
                .isDeleted(false)
                .createdAt(LocalDateTime.of(2026, 3, 31, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 31, 9, 0))
                .build();
    }

    // ─── createTransaction() tests ──────────────────────────────────

    @Test
    @DisplayName("createTransaction: authenticated admin creates transaction, returns correct response")
    void createTransaction_Success_ReturnsTransactionResponse() {
        User adminUser = buildAdminUser();
        setAuthenticatedUser(adminUser);
        TransactionRequest request = buildTransactionRequest();
        UUID savedId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        Transaction savedTransaction = buildTransaction(savedId, adminUser);

        when(transactionRepository.saveAndFlush(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponse response = transactionService.createTransaction(request);

        assertThat(response.getId()).isEqualTo(savedId);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("15000.50"));
        assertThat(response.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(response.getCategory()).isEqualTo("Salary");
        assertThat(response.getCreatedByName()).isEqualTo("Admin User");

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).saveAndFlush(txnCaptor.capture());

        Transaction capturedTransaction = txnCaptor.getValue();
        assertThat(capturedTransaction.getCreatedBy()).isEqualTo(adminUser);
        assertThat(capturedTransaction.getAmount()).isEqualByComparingTo(new BigDecimal("15000.50"));
    }

    @Test
    @DisplayName("createTransaction: no authentication in SecurityContext throws exception, repository never called")
    void createTransaction_NoAuthentication_ThrowsException() {
        SecurityContextHolder.clearContext();
        TransactionRequest request = buildTransactionRequest();

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(NullPointerException.class);

        verify(transactionRepository, never()).saveAndFlush(any());
    }

    // ─── getTransactionById() tests ─────────────────────────────────

    @Test
    @DisplayName("getTransactionById: existing non-deleted transaction returns correct response")
    void getTransactionById_ExistingId_ReturnsTransactionResponse() {
        User adminUser = buildAdminUser();
        UUID transactionId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        Transaction transaction = buildTransaction(transactionId, adminUser);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        TransactionResponse response = transactionService.getTransactionById(transactionId);

        assertThat(response.getId()).isEqualTo(transactionId);
        assertThat(response.getCategory()).isEqualTo("Salary");
        assertThat(response.getCreatedByName()).isEqualTo("Admin User");
    }

    @Test
    @DisplayName("getTransactionById: non-existent ID throws ResourceNotFoundException with UUID in message")
    void getTransactionById_NonExistentId_ThrowsResourceNotFoundException() {
        UUID missingId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

        when(transactionRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getTransactionById(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("dddddddd-dddd-dddd-dddd-dddddddddddd");
    }

    @Test
    @DisplayName("getTransactionById: soft-deleted transaction throws ResourceNotFoundException")
    void getTransactionById_SoftDeletedTransaction_ThrowsResourceNotFoundException() {
        User adminUser = buildAdminUser();
        UUID deletedId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        Transaction deletedTransaction = buildTransaction(deletedId, adminUser);
        deletedTransaction.setIsDeleted(true);

        when(transactionRepository.findById(deletedId)).thenReturn(Optional.of(deletedTransaction));

        assertThatThrownBy(() -> transactionService.getTransactionById(deletedId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    }

    // ─── deleteTransaction() (soft delete) tests ────────────────────

    @Test
    @DisplayName("deleteTransaction: admin user soft-deletes transaction, sets isDeleted=true and saves")
    void softDeleteTransaction_AdminUser_SetsIsDeletedTrue() {
        User adminUser = buildAdminUser();
        setAuthenticatedUser(adminUser);
        UUID transactionId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        Transaction transaction = buildTransaction(transactionId, adminUser);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        transactionService.deleteTransaction(transactionId);

        ArgumentCaptor<Transaction> txnCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txnCaptor.capture());

        Transaction capturedTransaction = txnCaptor.getValue();
        assertThat(capturedTransaction.getIsDeleted()).isTrue();
        assertThat(capturedTransaction.getId()).isEqualTo(transactionId);
    }

    @Test
    @DisplayName("deleteTransaction: non-existent transaction throws ResourceNotFoundException, save never called")
    void softDeleteTransaction_NonExistentTransaction_ThrowsResourceNotFoundException() {
        User adminUser = buildAdminUser();
        setAuthenticatedUser(adminUser);
        UUID missingId = UUID.fromString("99999999-9999-9999-9999-999999999999");

        when(transactionRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.deleteTransaction(missingId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).save(any());
    }
}
