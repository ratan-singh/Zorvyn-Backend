package com.zorvyn.financedashboard.repository;

import com.zorvyn.financedashboard.model.Transaction;
import com.zorvyn.financedashboard.model.enums.TransactionType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds dynamic JPA Specification predicates for filtering transactions.
 * Always includes the soft-delete exclusion filter — callers never need to remember it.
 */
public final class TransactionSpecification {

    private TransactionSpecification() {
    }

    public static Specification<Transaction> withFilters(
            TransactionType transactionType,
            String category,
            LocalDate startDate,
            LocalDate endDate) {

        return (Root<Transaction> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Soft-deleted records are always excluded from query results
            predicates.add(criteriaBuilder.isFalse(root.get("isDeleted")));

            if (transactionType != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), transactionType));
            }

            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), endDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
