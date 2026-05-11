package com.familie.cheltuieli_familie.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartFilters {

    private String category;
    private String person;
    private String dateRange;
    private String location;
}
