package com.proiect.controller;

import com.proiect.dto.RawInputDTO;
import com.proiect.service.ExpensePipelineService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final ExpensePipelineService pipelineService;

    @PostMapping("/process")
    public ResponseEntity<List<Long>> process(@Valid @RequestBody RawInputDTO input) {
        List<Long> ids = pipelineService.processRawInput(input.rawText());
        return ResponseEntity.ok(ids);
    }
}
