package com.proiect.service;

import dev.langchain4j.model.openai.OpenAiAudioModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
@RequiredArgsConstructor
public class VoiceService {

    private final OpenAiAudioModel whisperModel;

    public String transcribe(MultipartFile file) throws IOException {
        log.info("Transcribing audio file: {}", file.getOriginalFilename());
        
        // Whisper requires a file with a supported extension
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".wav";
        
        Path tempFile = Files.createTempFile("voice_", extension);
        try {
            file.transferTo(tempFile);
            // LangChain4j OpenAiAudioModel.transcribe returns Response<String>
            String transcript = whisperModel.transcribe(tempFile).content();
            log.debug("Transcript result: {}", transcript);
            return transcript;
        } catch (Exception e) {
            log.error("Failed to transcribe audio", e);
            throw new IOException("Transcription failed", e);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
