package com.finance_backend.investmentTracker.service;

import com.finance_backend.investmentTracker.dto.InvestmentRequest;
import com.finance_backend.investmentTracker.dto.InvestmentResponse;
import com.finance_backend.investmentTracker.dto.InvestmentSummaryResponse;
import com.finance_backend.investmentTracker.dto.InvestmentTypeBreakdown;
import com.finance_backend.investmentTracker.entity.Investment;
import com.finance_backend.investmentTracker.entity.InvestmentType;
import com.finance_backend.investmentTracker.exception.InvestmentNotFoundException;
import com.finance_backend.investmentTracker.repository.InvestmentRepository;
import com.finance_backend.auth.security.ResourceOwnershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class InvestmentServiceImpl implements InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final ResourceOwnershipGuard ownershipGuard;

    public InvestmentServiceImpl(InvestmentRepository investmentRepository,
                                 ResourceOwnershipGuard ownershipGuard) {
        this.investmentRepository = investmentRepository;
        this.ownershipGuard = ownershipGuard;
    }

    @Override
    @Transactional
    public InvestmentResponse createInvestment(InvestmentRequest request) {
        Investment investment = toEntity(request, new Investment());
        // No gain/loss yet if the user hasn't supplied a current value.
        if (investment.getCurrentValue() == null) {
            investment.setCurrentValue(investment.getInvestedAmount());
        }
        Investment saved = investmentRepository.save(investment);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InvestmentResponse getInvestmentById(Long id) {
        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new InvestmentNotFoundException(id));
        ownershipGuard.check(investment.getUserId(), "investment", id);
        return toResponse(investment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvestmentResponse> getInvestmentsByUserId(Long userId, InvestmentType type) {
        List<Investment> investments = (type != null)
                ? investmentRepository.findByUserIdAndType(userId, type)
                : investmentRepository.findByUserId(userId);

        return investments.stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(InvestmentResponse::getPurchaseDate).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InvestmentSummaryResponse getPortfolioSummary(Long userId) {
        List<Investment> investments = investmentRepository.findByUserId(userId);

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        Map<InvestmentType, List<Investment>> byType = new EnumMap<>(InvestmentType.class);

        for (Investment inv : investments) {
            totalInvested = totalInvested.add(inv.getInvestedAmount());
            totalCurrentValue = totalCurrentValue.add(inv.getCurrentValue());
            byType.computeIfAbsent(inv.getType(), t -> new ArrayList<>()).add(inv);
        }

        BigDecimal totalGainLoss = totalCurrentValue.subtract(totalInvested);
        double totalGainLossPercent = percentChange(totalInvested, totalGainLoss);

        List<InvestmentTypeBreakdown> breakdown = byType.entrySet().stream()
                .map(entry -> buildBreakdown(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(b -> b.getType().name()))
                .toList();

        return InvestmentSummaryResponse.builder()
                .userId(userId)
                .totalInvested(totalInvested)
                .totalCurrentValue(totalCurrentValue)
                .totalGainLossAmount(totalGainLoss)
                .totalGainLossPercent(totalGainLossPercent)
                .totalInvestmentCount(investments.size())
                .breakdownByType(breakdown)
                .build();
    }

    @Override
    @Transactional
    public InvestmentResponse updateInvestment(Long id, InvestmentRequest request) {
        Investment existing = investmentRepository.findById(id)
                .orElseThrow(() -> new InvestmentNotFoundException(id));
        // Before toEntity() -- toEntity overwrites userId from the request body,
        // so a later check would compare the caller against themselves.
        ownershipGuard.check(existing.getUserId(), "investment", id);

        Investment updated = toEntity(request, existing);
        if (updated.getCurrentValue() == null) {
            updated.setCurrentValue(updated.getInvestedAmount());
        }

        Investment saved = investmentRepository.save(updated);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteInvestment(Long id) {
        // findById, not existsById: the row has to be loaded to know who owns it.
        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new InvestmentNotFoundException(id));
        ownershipGuard.check(investment.getUserId(), "investment", id);
        investmentRepository.delete(investment);
    }

    // ---------- helpers ----------

    private InvestmentTypeBreakdown buildBreakdown(InvestmentType type, List<Investment> investments) {
        BigDecimal invested = investments.stream()
                .map(Investment::getInvestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal current = investments.stream()
                .map(Investment::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gainLoss = current.subtract(invested);

        return InvestmentTypeBreakdown.builder()
                .type(type)
                .count(investments.size())
                .investedAmount(invested)
                .currentValue(current)
                .gainLossAmount(gainLoss)
                .gainLossPercent(percentChange(invested, gainLoss))
                .build();
    }

    private double percentChange(BigDecimal base, BigDecimal change) {
        if (base.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return change.divide(base, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private Investment toEntity(InvestmentRequest request, Investment target) {
        target.setUserId(request.getUserId());
        target.setType(request.getType());
        target.setName(request.getName());
        target.setInvestedAmount(request.getInvestedAmount());
        target.setCurrentValue(request.getCurrentValue());
        target.setQuantity(request.getQuantity());
        target.setPurchaseDate(request.getPurchaseDate());
        target.setMaturityDate(request.getMaturityDate());
        target.setInterestRate(request.getInterestRate());
        target.setNotes(request.getNotes());
        return target;
    }

    private InvestmentResponse toResponse(Investment investment) {
        BigDecimal gainLoss = investment.getCurrentValue().subtract(investment.getInvestedAmount());
        double gainLossPercent = percentChange(investment.getInvestedAmount(), gainLoss);
        boolean matured = investment.getMaturityDate() != null
                && !investment.getMaturityDate().isAfter(LocalDate.now());

        return InvestmentResponse.builder()
                .id(investment.getId())
                .userId(investment.getUserId())
                .type(investment.getType())
                .name(investment.getName())
                .investedAmount(investment.getInvestedAmount())
                .currentValue(investment.getCurrentValue())
                .quantity(investment.getQuantity())
                .purchaseDate(investment.getPurchaseDate())
                .maturityDate(investment.getMaturityDate())
                .interestRate(investment.getInterestRate())
                .notes(investment.getNotes())
                .gainLossAmount(gainLoss)
                .gainLossPercent(gainLossPercent)
                .matured(matured)
                .createdAt(investment.getCreatedAt())
                .updatedAt(investment.getUpdatedAt())
                .build();
    }
}
