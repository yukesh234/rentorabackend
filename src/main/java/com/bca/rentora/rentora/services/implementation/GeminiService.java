package com.bca.rentora.rentora.services.implementation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import org.springframework.http.HttpHeaders;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;


@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apikey;

    private final RestTemplate restTemplate = new RestTemplate();
    public String generateFromFiles(MultipartFile[] files, String title, String category) throws IOException {
        List<Map<String, Object>> parts = new ArrayList<>();

        StringBuilder prompt = new StringBuilder(
                "Write a concise, appealing 2-3 sentence rental listing description based on these images. "
                        + "Do not invent specific claims (like 'brand new' or exact dimensions) that aren't visibly obvious."
        );
        if (title != null && !title.isBlank()) {
            prompt.append(" The listing title is: '").append(title).append("'.");
        }
        if (category != null && !category.isBlank()) {
            prompt.append(" It belongs to the '").append(category).append("' category.");
        }

        parts.add(Map.of("text", prompt.toString()));

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String base64 = Base64.getEncoder().encodeToString(file.getBytes());
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

            parts.add(Map.of(
                    "inline_data", Map.of(
                            "mime_type", mimeType,
                            "data", base64
                    )
            ));
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", parts))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=" + apikey;

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        JsonNode response = restTemplate.postForObject(url, entity, JsonNode.class);

        return response
                .path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();
    }
}
