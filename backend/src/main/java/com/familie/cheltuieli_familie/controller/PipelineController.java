package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.RawInputDTO;
import com.familie.cheltuieli_familie.service.ExpensePipelineService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
//sda
@RestController
@RequestMapping("/v1/pipeline")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PipelineController {

    private final ExpensePipelineService pipelineService;

    @PostMapping("/process")
    public ResponseEntity<List<Long>> process(@Valid @RequestBody RawInputDTO input) {
        List<Long> ids = pipelineService.processRawInput(input.rawText());
        return ResponseEntity.ok(ids);
    }
}
