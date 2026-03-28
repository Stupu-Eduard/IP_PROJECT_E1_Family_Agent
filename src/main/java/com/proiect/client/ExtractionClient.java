package com.proiect.client;

import com.proiect.dto.ExtractedExpenseDTO;
import com.proiect.dto.RawInputDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExtractionClient {

    private final RestTemplate restTemplate;
    private static final String EXTRACT_URL = "http://localhost:8080/api/v1/extract";

    public ExtractedExpenseDTO extractData(String rawText) {
        log.info("Sending raw text to Extraction API...");
        RawInputDTO request = new RawInputDTO(rawText);
        return restTemplate.postForObject(EXTRACT_URL, request, ExtractedExpenseDTO.class);
    }
}
