package org.ide.hack1.client.githubmodels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ide.hack1.dto.summary.SalesAggregatesDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GithubModelsClient {

    private static final Logger log = LoggerFactory.getLogger(GithubModelsClient.class);
    private String token;
    private String modelsUrl;
    private String modelId;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public GithubModelsClient(
            @Value("${GITHUB_TOKEN:}") String token,
            @Value("${GITHUB_MODELS_URL:https://api.github.com/ai/experimental/models}") String modelsUrl,
            @Value("${MODEL_ID:}") String modelId
    ) {
        this.token = token;
        this.modelsUrl = modelsUrl;
        this.modelId = modelId;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // If token or modelId are not provided via env/properties, attempt to load from a .env file in project root
        if ((this.token == null || this.token.isBlank()) || (this.modelId == null || this.modelId.isBlank())) {
            try {
                Path envPath = Path.of(".env");
                if (Files.exists(envPath)) {
                    List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
                    for (String line : lines) {
                        String l = line.trim();
                        if (l.isEmpty() || l.startsWith("#")) continue;
                        int idx = l.indexOf('=');
                        if (idx <= 0) continue;
                        String k = l.substring(0, idx).trim();
                        String v = l.substring(idx + 1).trim();
                        // remove optional surrounding quotes
                        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                            v = v.substring(1, v.length() - 1);
                        }
                        switch (k) {
                            case "GITHUB_TOKEN":
                                if (this.token == null || this.token.isBlank()) this.token = v;
                                break;
                            case "MODEL_ID":
                                if (this.modelId == null || this.modelId.isBlank()) this.modelId = v;
                                break;
                            case "GITHUB_MODELS_URL":
                                if (this.modelsUrl == null || this.modelsUrl.isBlank()) this.modelsUrl = v;
                                break;
                            default:
                                break;
                        }
                    }
                    log.info("Loaded credentials from .env for GitHub Models: tokenConfigured={} modelConfigured={}", (this.token != null && !this.token.isBlank()), (this.modelId != null && !this.modelId.isBlank()));
                }
            } catch (IOException ex) {
                log.warn("Failed to read .env file: {}", ex.getMessage());
            }
        }
    }

    /**
     * Genera un summary llamando al GitHub Models API. Devuelve null si no está configurado o falla.
     */
    public String generateSummary(SalesAggregatesDTO agg, LocalDate from, LocalDate to) {
        if (token == null || token.isBlank()) {
            log.warn("GITHUB_TOKEN not configured, skipping LLM call");
            return null;
        }
        if (modelId == null || modelId.isBlank()) {
            log.warn("MODEL_ID not configured, skipping LLM call");
            return null;
        }

        try {
            // Construct messages per README and include date range
            String system = "Eres un analista que escribe resúmenes breves y claros para emails corporativos en español. Sé conciso (<=120 palabras).";
            String user = String.format("Periodo: %s a %s. Con estos datos: totalUnits=%d, totalRevenue=%.2f, topSku=%s, topBranch=%s. Devuelve un resumen ≤120 palabras para enviar por email.",
                    from != null ? from.toString() : "N/A",
                    to != null ? to.toString() : "N/A",
                    agg.getTotalUnits(), agg.getTotalRevenue(), agg.getTopSku(), agg.getTopBranch());

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", modelId);

            Map<String, String> sys = new HashMap<>(); sys.put("role","system"); sys.put("content", system);
            Map<String, String> usr = new HashMap<>(); usr.put("role","user"); usr.put("content", user);
            payload.put("messages", new Map[]{sys, usr});
            payload.put("max_tokens", 200);

            String body = mapper.writeValueAsString(payload);

            URI uri = URI.create(modelsUrl);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            int sc = resp.statusCode();
            if (sc >= 200 && sc < 300) {
                String respBody = resp.body();
                // Try to parse common response shapes
                JsonNode root = mapper.readTree(respBody);
                // Try path: choices[0].message.content
                JsonNode content = null;
                if (root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()) {
                    JsonNode choice0 = root.get("choices").get(0);
                    if (choice0.has("message") && choice0.get("message").has("content")) {
                        content = choice0.get("message").get("content");
                    } else if (choice0.has("text")) {
                        content = choice0.get("text");
                    }
                }
                // Fallback: data[0].content
                if (content == null && root.has("data") && root.get("data").isArray() && !root.get("data").isEmpty()) {
                    JsonNode d0 = root.get("data").get(0);
                    if (d0.has("content")) content = d0.get("content");
                }
                if (content != null) {
                    String text = content.isTextual() ? content.asText() : content.toString();
                    return text.trim();
                } else {
                    log.warn("LLM response did not contain expected fields: {}", root.toString());
                    return null;
                }
            } else {
                log.error("LLM request failed with status {} and body: {}", sc, resp.body());
                return null;
            }
        } catch (Exception ex) {
            log.error("Error calling GitHub Models API: {}", ex.getMessage(), ex);
            return null;
        }
    }
}
