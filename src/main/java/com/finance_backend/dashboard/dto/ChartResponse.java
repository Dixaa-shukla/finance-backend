package com.finance_backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Uses the same Chart.js format for expense, income, and savings charts,
 * so the frontend can use this response directly without extra changes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartResponse {

    private List<String> labels;
    private List<ChartDataset> datasets;
}
