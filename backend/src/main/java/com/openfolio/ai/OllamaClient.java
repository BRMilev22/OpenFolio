package com.openfolio.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Thin client for the local Ollama API running on port 11434.
 * Uses Java 21 built-in HttpClient — no extra dependencies needed.
 * Model: qwen2.5:14b (best available for professional text generation).
 */
@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    public static final String MODEL = "qwen2.5:14b";
    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private static final Duration TIMEOUT = Duration.ofSeconds(120);

    private final HttpClient http;
    private final ObjectMapper mapper;

    public OllamaClient(ObjectMapper mapper) {
        this.mapper = mapper;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Send a chat request to Ollama and return the assistant's text response.
     * Returns {@code null} silently if Ollama is unavailable or times out.
     */
    public String chat(String systemPrompt, String userMessage, int maxTokens) {
        try {
            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "stream", false,
                    "options", Map.of("num_predict", maxTokens),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user",   "content", userMessage)
                    )
            );

            String json = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Ollama returned HTTP {}", response.statusCode());
                return null;
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode content = root.path("message").path("content");
            if (content.isMissingNode()) return null;

            String text = content.asText().trim();
            // Strip any markdown fences the model might include
            text = text.replaceAll("(?m)^```[a-z]*\\n?", "").replace("```", "").trim();
            log.info("Ollama [{}] → {} chars", MODEL, text.length());
            return text.isBlank() ? null : text;

        } catch (java.net.ConnectException e) {
            log.warn("Ollama not reachable at {} — AI enhancement skipped", OLLAMA_URL);
        } catch (Exception e) {
            log.warn("Ollama error: {}", e.getMessage());
        }
        return null;
    }
}
