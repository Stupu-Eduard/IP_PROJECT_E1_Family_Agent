package com.proiect.controller;

import com.proiect.dto.RawInputDTO;
import com.proiect.service.ExpensePipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final ExpensePipelineService pipelineService;

    @PostMapping("/process")
    public ResponseEntity<String> process(@RequestBody RawInputDTO input) {
        Long id = pipelineService.processRawInput(input.rawText());
        return ResponseEntity.ok(String.format("Entity created with ID: %d and replicated in Qdrant.", id));
    }
}
