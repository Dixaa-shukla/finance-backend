package com.finance_backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/** Mirrors a single Chart.js dataset: { label, data }. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartDataset {

    private String label;
    private List<BigDecimal> data;
}
