package com.finance_backend.income.service;

import com.finance_backend.category.entity.Category;
import com.finance_backend.category.entity.CategoryType;
import com.finance_backend.category.repository.CategoryRepository;
import com.finance_backend.common.exception.BadRequestException;
import com.finance_backend.income.dto.IncomeFilterRequest;
import com.finance_backend.income.dto.IncomeRequest;
import com.finance_backend.income.dto.IncomeResponse;
import com.finance_backend.income.entity.Income;
import com.finance_backend.income.exception.IncomeNotFoundException;
import com.finance_backend.income.repository.IncomeRepository;
import com.finance_backend.income.repository.IncomeSpecification;
import com.finance_backend.auth.security.ResourceOwnershipGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class IncomeServiceImpl implements IncomeService {

    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;
    private final ResourceOwnershipGuard ownershipGuard;

    public IncomeServiceImpl(IncomeRepository incomeRepository,
                             CategoryRepository categoryRepository,
                             ResourceOwnershipGuard ownershipGuard) {
        this.incomeRepository = incomeRepository;
        this.categoryRepository = categoryRepository;
        this.ownershipGuard = ownershipGuard;
    }

    @Override
    @Transactional
    public IncomeResponse createIncome(IncomeRequest request) {
        validateCategory(request.getCategoryId());
        Income income = toEntity(request, new Income());
        Income saved = incomeRepository.save(income);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeResponse getIncomeById(Long id) {
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new IncomeNotFoundException(id));
        ownershipGuard.check(income.getUserId(), "income", id);
        return toResponse(income);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncomeResponse> getIncomesByUserId(Long userId) {
        return incomeRepository.findByUserIdOrderByIncomeDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncomeResponse> searchIncomes(Long userId, IncomeFilterRequest filter, Pageable pageable) {
        return incomeRepository
                .findAll(IncomeSpecification.byUserAndFilters(userId, filter), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public IncomeResponse updateIncome(Long id, IncomeRequest request) {
        validateCategory(request.getCategoryId());
        Income existing = incomeRepository.findById(id)
                .orElseThrow(() -> new IncomeNotFoundException(id));
        // Before toEntity() -- toEntity overwrites userId from the request body,
        // so a later check would compare the caller against themselves.
        ownershipGuard.check(existing.getUserId(), "income", id);
        Income updated = toEntity(request, existing);
        Income saved = incomeRepository.save(updated);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteIncome(Long id) {
        // findById, not existsById: the row has to be loaded to know who owns it.
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new IncomeNotFoundException(id));
        ownershipGuard.check(income.getUserId(), "income", id);
        incomeRepository.delete(income);
    }

    // ---------- helpers ----------

    /** If a categoryId is supplied, it must exist and be typed INCOME. */
    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BadRequestException("categoryId does not reference an existing category: " + categoryId));

        if (category.getType() != CategoryType.INCOME) {
            throw new BadRequestException("categoryId " + categoryId + " is not an INCOME category");
        }
    }

    private Income toEntity(IncomeRequest request, Income target) {
        target.setUserId(request.getUserId());
        target.setAmount(request.getAmount());
        target.setSource(request.getSource());
        target.setCategoryId(request.getCategoryId());
        target.setIncomeDate(request.getIncomeDate());
        target.setNotes(request.getNotes());
        target.setRecurring(request.isRecurring());
        return target;
    }

    private IncomeResponse toResponse(Income income) {
        String categoryName = null;
        if (income.getCategoryId() != null) {
            Optional<Category> category = categoryRepository.findById(income.getCategoryId());
            categoryName = category.map(Category::getName).orElse(null);
        }

        return IncomeResponse.builder()
                .id(income.getId())
                .userId(income.getUserId())
                .amount(income.getAmount())
                .source(income.getSource())
                .categoryId(income.getCategoryId())
                .categoryName(categoryName)
                .incomeDate(income.getIncomeDate())
                .notes(income.getNotes())
                .isRecurring(income.isRecurring())
                .createdAt(income.getCreatedAt())
                .updatedAt(income.getUpdatedAt())
                .build();
    }
}
