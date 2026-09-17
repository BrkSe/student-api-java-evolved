package com.burakkutbay.studentapi.security;

import java.util.Random;

import com.burakkutbay.studentapi.util.StringUtils;

/**
 * Yazma işlemleri için X-API-Key doğrulaması yapar.
 */
public class ApiKeyService {

    private final Random random = new Random();

    private final String apiKey;

    public ApiKeyService(String configuredKey) {
        if (StringUtils.isBlank(configuredKey)) {
            this.apiKey = generateKey();
        } else {
            this.apiKey = configuredKey.trim();
        }
    }

    public String generateKey() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public boolean isValid(String providedKey) {
        if (providedKey == null) {
            return false;
        }
        return apiKey.equals(providedKey.trim());
    }

    public String getApiKey() {
        return apiKey;
    }

    public String maskedKey() {
        if (apiKey.length() <= 4) {
            return StringUtils.repeat("*", apiKey.length());
        }
        return apiKey.substring(0, 4) + StringUtils.repeat("*", apiKey.length() - 4);
    }
}
