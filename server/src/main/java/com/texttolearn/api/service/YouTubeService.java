package com.texttolearn.api.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.texttolearn.api.config.AppProperties;
import com.texttolearn.api.dto.VideoResult;

@Service
public class YouTubeService {
    private final AppProperties properties;
    private final WebClient webClient;

    public YouTubeService(AppProperties properties, WebClient.Builder builder) {
        this.properties = properties;
        this.webClient = builder.baseUrl("https://www.googleapis.com/youtube/v3").build();
    }

    @SuppressWarnings("unchecked")
    public VideoResult search(String query) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("Video query is required.");
        }

        if (!StringUtils.hasText(properties.youtubeApiKey())) {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            return new VideoResult(
                    "Search YouTube: " + query,
                    null,
                    null,
                    "https://www.youtube.com/results?search_query=" + encoded
            );
        }

        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", query)
                        .queryParam("maxResults", 1)
                        .queryParam("type", "video")
                        .queryParam("videoEmbeddable", "true")
                        .queryParam("key", properties.youtubeApiKey())
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> items = Optional.ofNullable((List<Map<String, Object>>) response.get("items"))
                .orElse(List.of());
        if (items.isEmpty()) {
            return new VideoResult("No video found", null, null, null);
        }

        Map<String, Object> first = items.get(0);
        Map<String, Object> id = (Map<String, Object>) first.get("id");
        Map<String, Object> snippet = (Map<String, Object>) first.get("snippet");
        String videoId = (String) id.get("videoId");
        String title = (String) snippet.get("title");
        return new VideoResult(
                title,
                videoId,
                "https://www.youtube.com/embed/" + videoId,
                "https://www.youtube.com/watch?v=" + videoId
        );
    }
}

