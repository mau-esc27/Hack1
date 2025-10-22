package org.ide.hack1.client.githubmodels;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class GithubModelsClient {

    private final String githubUrl;
    private final String modelId;
    private final String githubToken;
    private final HttpClient httpClient;

    public GithubModelsClient(
            @Value("${llm.github.url:https://api.github.com}") String githubUrl,
            @Value("${llm.github.model-id}") String modelId,
            @Value("${llm.github.token}") String githubToken
    ) {
        this.githubUrl = githubUrl;
        this.modelId = modelId;
        this.githubToken = githubToken;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String generateSummary(String prompt) {
        try {
            String endpoint = String.format("%s/models/%s/generate", githubUrl, modelId);

            String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "Eres un analista experto de marketing de Oreo. Responde en español, breve y formal."},
                    {"role": "user", "content": "%s"}
                  ]
                }
                """.formatted(modelId, escapeJson(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + githubToken)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return extractText(response.body());
            } else {
                return "Error al comunicarse con GitHub Models. Código: " + response.statusCode();
            }

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error al conectar con GitHub Models: " + e.getMessage();
        } catch (Exception ex) {
            return "Fallo inesperado: " + ex.getMessage();
        }
    }

    private String extractText(String json) {
        try {
            int idx = json.indexOf("\"content\":");
            if (idx == -1) return json;
            int start = json.indexOf("\"", idx + 10);
            int end = json.indexOf("\"", start + 1);
            if (start == -1 || end == -1) return json;
            return json.substring(start + 1, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"");
        } catch (Exception e) {
            return json;
        }
    }

    private String escapeJson(String text) {
        return text.replace("\"", "\\\"");
    }
}