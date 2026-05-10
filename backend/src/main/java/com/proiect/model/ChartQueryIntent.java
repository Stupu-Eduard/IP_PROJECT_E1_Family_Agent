package com.proiect.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartQueryIntent {

    private String responseType;
    private String chartType;
    private String aggregation;
    private String groupBy;
    private String seriesBy;
    private String title;
    private ChartFilters filters;
}
