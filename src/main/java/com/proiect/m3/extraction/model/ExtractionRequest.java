package com.proiect.m3.extraction.model;

public class ExtractionRequest {
    private String rawText;

    public ExtractionRequest() {}

    public ExtractionRequest(String rawText) {
        this.rawText = rawText;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}
