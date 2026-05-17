package com.familie.cheltuieli_familie.config;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryConfigTest {

    @InjectMocks
    private CloudinaryConfig cloudinaryConfig;

    @Test
    void cloudinaryBean_shouldBeCreated_whenCredentialsProvided() {
        ReflectionTestUtils.setField(cloudinaryConfig, "cloudName", "test-cloud");
        ReflectionTestUtils.setField(cloudinaryConfig, "apiKey", "test-key");
        ReflectionTestUtils.setField(cloudinaryConfig, "apiSecret", "test-secret");

        Cloudinary cloudinary = cloudinaryConfig.cloudinary();

        assertNotNull(cloudinary);
        assertEquals("test-cloud", cloudinary.config.cloudName);
        assertEquals("test-key", cloudinary.config.apiKey);
        assertEquals("test-secret", cloudinary.config.apiSecret);
    }

    @Test
    void cloudinaryBean_shouldBeCreated_whenCredentialsEmpty() {
        ReflectionTestUtils.setField(cloudinaryConfig, "cloudName", "");
        ReflectionTestUtils.setField(cloudinaryConfig, "apiKey", "");
        ReflectionTestUtils.setField(cloudinaryConfig, "apiSecret", "");

        Cloudinary cloudinary = cloudinaryConfig.cloudinary();

        assertNotNull(cloudinary);
    }

    @Test
    void cloudinaryBean_shouldBeCreated_whenCredentialsNull() {
        ReflectionTestUtils.setField(cloudinaryConfig, "cloudName", (String) null);
        ReflectionTestUtils.setField(cloudinaryConfig, "apiKey", (String) null);
        ReflectionTestUtils.setField(cloudinaryConfig, "apiSecret", (String) null);

        Cloudinary cloudinary = cloudinaryConfig.cloudinary();

        assertNotNull(cloudinary);
    }
}
