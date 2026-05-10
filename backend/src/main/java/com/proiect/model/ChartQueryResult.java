package com.proiect.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartQueryResult {

    private List<Map<String, Object>> rows;
    private List<String> seriesNames;
    private String labelKey;
}
