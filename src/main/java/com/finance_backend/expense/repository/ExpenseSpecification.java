package com.finance_backend.expense.repository;

import com.finance_backend.expense.dto.ExpenseFilterRequest;
import com.finance_backend.expense.entity.Expense;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * dynamic JPA Specification from an ExpenseFilterRequest.
 * Any null/blank field in the filter is skipped.
 */
public final class ExpenseSpecification {

    private ExpenseSpecification() {
    }

    public static Specification<Expense> byUserAndFilters(Long userId, ExpenseFilterRequest filter) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("userId"), userId));

            if (filter != null) {
                if (StringUtils.hasText(filter.getCategory())) {
                    predicates.add(cb.equal(cb.lower(root.get("category")), filter.getCategory().toLowerCase()));
                }
                if (filter.getPaymentMethod() != null) {
                    predicates.add(cb.equal(root.get("paymentMethod"), filter.getPaymentMethod()));
                }
                if (StringUtils.hasText(filter.getMerchant())) {
                    predicates.add(cb.like(cb.lower(root.get("merchant")),
                            "%" + filter.getMerchant().toLowerCase() + "%"));
                }
                if (filter.getStartDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), filter.getStartDate()));
                }
                if (filter.getEndDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), filter.getEndDate()));
                }
                if (filter.getMinAmount() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), filter.getMinAmount()));
                }
                if (filter.getMaxAmount() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("amount"), filter.getMaxAmount()));
                }
                if (StringUtils.hasText(filter.getTag())) {
                    predicates.add(cb.isMember(filter.getTag(), root.get("tags")));
                }
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
