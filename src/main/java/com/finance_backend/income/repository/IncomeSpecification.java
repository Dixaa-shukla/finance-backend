package com.finance_backend.income.repository;

import com.finance_backend.income.dto.IncomeFilterRequest;
import com.finance_backend.income.entity.Income;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class IncomeSpecification {

    private IncomeSpecification() {
    }

    public static Specification<Income> byUserAndFilters(Long userId, IncomeFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("userId"), userId));

            if (filter != null) {
                if (filter.getSource() != null) {
                    predicates.add(cb.equal(root.get("source"), filter.getSource()));
                }
                if (filter.getCategoryId() != null) {
                    predicates.add(cb.equal(root.get("categoryId"), filter.getCategoryId()));
                }
                if (filter.getStartDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("incomeDate"), filter.getStartDate()));
                }
                if (filter.getEndDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("incomeDate"), filter.getEndDate()));
                }
                if (filter.getMinAmount() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), filter.getMinAmount()));
                }
                if (filter.getMaxAmount() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("amount"), filter.getMaxAmount()));
                }
                if (filter.getIsRecurring() != null) {
                    predicates.add(cb.equal(root.get("isRecurring"), filter.getIsRecurring()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}