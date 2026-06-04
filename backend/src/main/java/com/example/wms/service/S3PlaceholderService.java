package com.example.wms.service;

import com.example.wms.config.S3Properties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class S3PlaceholderService {
    private final S3Properties properties;

    public S3PlaceholderService(S3Properties properties) {
        this.properties = properties;
    }

    public String publicUrl(String key) {
        if (!StringUtils.hasText(key) || !StringUtils.hasText(properties.getBucket())) {
            return key;
        }
        return "https://" + properties.getBucket() + ".s3." + properties.getRegion() + ".amazonaws.com/" + key;
    }
}
