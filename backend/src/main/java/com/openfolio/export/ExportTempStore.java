package com.openfolio.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * In-memory store for generated PDFs with automatic expiry (10 minutes).
 */
@Component
public class ExportTempStore {

    private static final Logger log = LoggerFactory.getLogger(ExportTempStore.class);
    private final Map<String, byte[]> store = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public String store(byte[] pdfBytes) {
        String token = UUID.randomUUID().toString().replace("-", "");
        store.put(token, pdfBytes);
        scheduler.schedule(() -> {
            store.remove(token);
            log.debug("PDF export token {} expired", token);
        }, 10, TimeUnit.MINUTES);
        return token;
    }

    public byte[] retrieve(String token) {
        return store.get(token);
    }
}
