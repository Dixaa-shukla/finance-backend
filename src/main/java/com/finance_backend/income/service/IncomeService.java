package com.finance_backend.income.service;

import com.finance_backend.income.dto.IncomeFilterRequest;
import com.finance_backend.income.dto.IncomeRequest;
import com.finance_backend.income.dto.IncomeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IncomeService {

    IncomeResponse createIncome(IncomeRequest request);

    IncomeResponse getIncomeById(Long id);

    List<IncomeResponse> getIncomesByUserId(Long userId);

    Page<IncomeResponse> searchIncomes(Long userId, IncomeFilterRequest filter, Pageable pageable);

    IncomeResponse updateIncome(Long id, IncomeRequest request);

    void deleteIncome(Long id);
}
