package com.familie.cheltuieli_familie.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Uploads a file (image or PDF) to Cloudinary.
     *
     * @param file     the file to upload
     * @param folder   the Cloudinary folder path (e.g., "receipts/2026-05")
     * @param fileName a descriptive name for the file
     * @return the secure URL of the uploaded file
     */
    public String uploadFile(File file, String folder, String fileName) {
        try {
            log.info("Uploading file to Cloudinary: {} ({} bytes)", fileName, file.length());

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", sanitizePublicId(fileName),
                    "resource_type", detectResourceType(file),
                    "overwrite", false
            ));

            String url = (String) uploadResult.get("secure_url");
            log.info("Cloudinary upload successful: {}", url);
            return url;

        } catch (IOException e) {
            log.error("Cloudinary upload failed for file: {}", fileName, e);
            throw new RuntimeException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a file from Cloudinary by its public ID.
     *
     * @param publicId the Cloudinary public ID
     */
    public void deleteFile(String publicId) {
        try {
            log.info("Deleting file from Cloudinary: {}", publicId);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.error("Cloudinary delete failed for publicId: {}", publicId, e);
        }
    }

    private String detectResourceType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pdf")) {
            return "raw";
        }
        return "image";
    }

    private String sanitizePublicId(String fileName) {
        return fileName
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_{2,}", "_")
                .substring(0, Math.min(fileName.length(), 100));
    }
}
