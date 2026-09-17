package com.burakkutbay.studentapi.http;

import java.util.UUID;

/// İstek boyunca değişmeyen bağlam. `ScopedValue` ile yalnızca isteği işleyen kod bloğuna bağlanır;
/// `ThreadLocal`'ın aksine sızıntı ve temizleme sorunu yoktur, sanal thread'lerle ucuzdur.
public record RequestContext(String requestId, String method, String path) {

    public static final ScopedValue<RequestContext> CURRENT = ScopedValue.newInstance();

    public static RequestContext of(String method, String path) {
        return new RequestContext(UUID.randomUUID().toString().substring(0, 8), method, path);
    }

    /// Log satırları için kısa önek; bağlam yoksa boş.
    public static String logPrefix() {
        return CURRENT.isBound() ? "[" + CURRENT.get().requestId() + "] " : "";
    }
}
