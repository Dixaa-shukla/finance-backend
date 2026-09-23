package com.finance_backend.recurring.service;

import com.finance_backend.common.exception.BadRequestException;
import com.finance_backend.expense.dto.ExpenseRequest;
import com.finance_backend.expense.entity.PaymentMethod;
import com.finance_backend.expense.service.ExpenseService;
import com.finance_backend.income.dto.IncomeRequest;
import com.finance_backend.income.service.IncomeService;
import com.finance_backend.recurring.dto.RecurringTransactionRequest;
import com.finance_backend.recurring.dto.RecurringTransactionResponse;
import com.finance_backend.recurring.entity.RecurringFrequency;
import com.finance_backend.recurring.entity.RecurringTransaction;
import com.finance_backend.recurring.entity.RecurringTransactionType;
import com.finance_backend.recurring.exception.RecurringTransactionNotFoundException;
import com.finance_backend.recurring.repository.RecurringTransactionRepository;
import com.finance_backend.auth.security.ResourceOwnershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringTransactionServiceImpl implements RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final ExpenseService expenseService;
    private final IncomeService incomeService;
    private final ResourceOwnershipGuard ownershipGuard;

    public RecurringTransactionServiceImpl(RecurringTransactionRepository recurringTransactionRepository,
                                           ExpenseService expenseService,
                                           IncomeService incomeService,
                                           ResourceOwnershipGuard ownershipGuard) {
        this.recurringTransactionRepository = recurringTransactionRepository;
        this.expenseService = expenseService;
        this.incomeService = incomeService;
        this.ownershipGuard = ownershipGuard;
    }

    @Override
    @Transactional
    public RecurringTransactionResponse createRecurringTransaction(RecurringTransactionRequest request) {
        validateTypeSpecificFields(request);

        RecurringTransaction rt = toEntity(request, new RecurringTransaction());
        rt.setNextDueDate(request.getStartDate());
        rt.setActive(true);

        RecurringTransaction saved = recurringTransactionRepository.save(rt);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringTransactionResponse getRecurringTransactionById(Long id) {
        RecurringTransaction rt = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new RecurringTransactionNotFoundException(id));
        ownershipGuard.check(rt.getUserId(), "recurring transaction", id);
        return toResponse(rt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getRecurringTransactionsByUserId(Long userId) {
        return recurringTransactionRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RecurringTransactionResponse updateRecurringTransaction(Long id, RecurringTransactionRequest request) {
        validateTypeSpecificFields(request);

        RecurringTransaction existing = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new RecurringTransactionNotFoundException(id));
        // Before toEntity() -- toEntity overwrites userId from the request body,
        // so a later check would compare the caller against themselves.
        ownershipGuard.check(existing.getUserId(), "recurring transaction", id);

        RecurringTransaction updated = toEntity(request, existing);
        // Don't reset the next due date when editing details like amount or title.
// The schedule should continue from its current timing.

        RecurringTransaction saved = recurringTransactionRepository.save(updated);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public RecurringTransactionResponse pauseRecurringTransaction(Long id) {
        RecurringTransaction rt = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new RecurringTransactionNotFoundException(id));
        ownershipGuard.check(rt.getUserId(), "recurring transaction", id);
        rt.setActive(false);
        return toResponse(recurringTransactionRepository.save(rt));
    }

    @Override
    @Transactional
    public RecurringTransactionResponse resumeRecurringTransaction(Long id) {
        RecurringTransaction rt = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new RecurringTransactionNotFoundException(id));
        ownershipGuard.check(rt.getUserId(), "recurring transaction", id);
        rt.setActive(true);
        return toResponse(recurringTransactionRepository.save(rt));
    }

    @Override
    @Transactional
    public void deleteRecurringTransaction(Long id) {
        // findById, not existsById: the row has to be loaded to know who owns it.
        RecurringTransaction rt = recurringTransactionRepository.findById(id)
                .orElseThrow(() -> new RecurringTransactionNotFoundException(id));
        ownershipGuard.check(rt.getUserId(), "recurring transaction", id);
        recurringTransactionRepository.delete(rt);
    }

    @Override
    @Transactional
    public int processDueRecurringTransactions() {
        LocalDate today = LocalDate.now();
        List<RecurringTransaction> dueRules = recurringTransactionRepository
                .findByIsActiveTrueAndNextDueDateLessThanEqual(today);

        int generatedCount = 0;

        for (RecurringTransaction rt : dueRules) {
            // Catch up on every missed occurrence, not just one, in case the
            // scheduler didn't run for a while (server downtime, etc.).
            while (rt.isActive() && !rt.getNextDueDate().isAfter(today)) {
                generateTransactionFor(rt, rt.getNextDueDate());
                generatedCount++;

                LocalDate next = advance(rt.getNextDueDate(), rt.getFrequency());
                if (rt.getEndDate() != null && next.isAfter(rt.getEndDate())) {
                    rt.setActive(false);
                }
                rt.setNextDueDate(next);
            }
            recurringTransactionRepository.save(rt);
        }

        return generatedCount;
    }

    // ---------- helpers ----------

    private void validateTypeSpecificFields(RecurringTransactionRequest request) {
        if (request.getType() == RecurringTransactionType.EXPENSE && !StringUtils.hasText(request.getCategory())) {
            throw new BadRequestException("category is required when type is EXPENSE");
        }
        if (request.getType() == RecurringTransactionType.INCOME && request.getIncomeSource() == null) {
            throw new BadRequestException("incomeSource is required when type is INCOME");
        }
    }

    private LocalDate advance(LocalDate date, RecurringFrequency frequency) {
        return switch (frequency) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case YEARLY -> date.plusYears(1);
        };
    }

    /** Delegates to the existing ExpenseService/IncomeService so their validation logic runs unchanged. */
    private void generateTransactionFor(RecurringTransaction rt, LocalDate occurrenceDate) {
        String autoNote = "Auto-generated from recurring rule: " + rt.getTitle();

        if (rt.getType() == RecurringTransactionType.EXPENSE) {
            ExpenseRequest expenseRequest = ExpenseRequest.builder()
                    .userId(rt.getUserId())
                    .amount(rt.getAmount())
                    .category(rt.getCategory())
                    .merchant(rt.getTitle())
                    .expenseDate(occurrenceDate)
                    .paymentMethod(rt.getPaymentMethod() != null ? rt.getPaymentMethod() : PaymentMethod.OTHER)
                    .notes(rt.getNotes() != null ? rt.getNotes() : autoNote)
                    .build();
            expenseService.createExpense(expenseRequest);
        } else {
            IncomeRequest incomeRequest = IncomeRequest.builder()
                    .userId(rt.getUserId())
                    .amount(rt.getAmount())
                    .source(rt.getIncomeSource())
                    .incomeDate(occurrenceDate)
                    .notes(rt.getNotes() != null ? rt.getNotes() : autoNote)
                    .isRecurring(true)
                    .build();
            incomeService.createIncome(incomeRequest);
        }
    }

    private RecurringTransaction toEntity(RecurringTransactionRequest request, RecurringTransaction target) {
        target.setUserId(request.getUserId());
        target.setType(request.getType());
        target.setTitle(request.getTitle());
        target.setAmount(request.getAmount());
        target.setCategory(request.getCategory());
        target.setIncomeSource(request.getIncomeSource());
        target.setPaymentMethod(request.getPaymentMethod());
        target.setFrequency(request.getFrequency());
        target.setStartDate(request.getStartDate());
        target.setEndDate(request.getEndDate());
        target.setNotes(request.getNotes());
        return target;
    }

    private RecurringTransactionResponse toResponse(RecurringTransaction rt) {
        return RecurringTransactionResponse.builder()
                .id(rt.getId())
                .userId(rt.getUserId())
                .type(rt.getType())
                .title(rt.getTitle())
                .amount(rt.getAmount())
                .category(rt.getCategory())
                .incomeSource(rt.getIncomeSource())
                .paymentMethod(rt.getPaymentMethod())
                .frequency(rt.getFrequency())
                .startDate(rt.getStartDate())
                .nextDueDate(rt.getNextDueDate())
                .endDate(rt.getEndDate())
                .isActive(rt.isActive())
                .notes(rt.getNotes())
                .createdAt(rt.getCreatedAt())
                .updatedAt(rt.getUpdatedAt())
                .build();
    }
}
