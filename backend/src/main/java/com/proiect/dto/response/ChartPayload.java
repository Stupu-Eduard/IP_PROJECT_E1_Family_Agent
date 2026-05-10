package com.proiect.dto.response;

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
public class ChartPayload {

    private String chartType;
    private String title;
    private List<Map<String, Object>> data;
    private List<String> dataKeys;
    private String xAxisKey;
}
