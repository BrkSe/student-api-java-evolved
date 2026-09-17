package com.burakkutbay.studentapi.security;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

import com.burakkutbay.studentapi.util.StringUtils;

/// Yazma işlemleri için `X-API-Key` doğrulaması yapar.
public final class ApiKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String apiKey;

    public ApiKeyService(String configuredKey) {
        this.apiKey = StringUtils.isBlank(configuredKey) ? generateKey() : configuredKey.strip();
    }

    public static String generateKey() {
        var bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /// Sabit zamanlı karşılaştırma; zamanlama saldırılarıyla anahtar tahmin edilemez.
    public boolean isValid(String providedKey) {
        return providedKey != null
                && MessageDigest.isEqual(apiKey.getBytes(UTF_8), providedKey.strip().getBytes(UTF_8));
    }

    public String maskedKey() {
        if (apiKey.length() <= 4) {
            return "*".repeat(apiKey.length());
        }
        return apiKey.substring(0, 4) + "*".repeat(apiKey.length() - 4);
    }
}
