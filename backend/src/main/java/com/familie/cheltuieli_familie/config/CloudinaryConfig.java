package com.familie.cheltuieli_familie.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@Slf4j
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        String resolvedCloudName = KeyResolver.resolve(cloudName, "CLOUDINARY_CLOUD_NAME");
        String resolvedApiKey = KeyResolver.resolve(apiKey, "CLOUDINARY_API_KEY");
        String resolvedApiSecret = KeyResolver.resolve(apiSecret, "CLOUDINARY_API_SECRET");

        if (resolvedCloudName.isEmpty() || resolvedApiKey.isEmpty() || resolvedApiSecret.isEmpty()) {
            log.warn("Cloudinary credentials not fully configured. Uploads will fail.");
        }

        Map<String, String> config = ObjectUtils.asMap(
                "cloud_name", resolvedCloudName,
                "api_key", resolvedApiKey,
                "api_secret", resolvedApiSecret,
                "secure", true
        );

        return new Cloudinary(config);
    }
}
