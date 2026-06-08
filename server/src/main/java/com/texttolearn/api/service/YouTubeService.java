package com.texttolearn.api.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
                        .queryParam("q", query + " tutorial -shorts")
                        .queryParam("maxResults", 8)
                        .queryParam("order", "relevance")
                        .queryParam("videoCategoryId", 27)
                        .queryParam("relevanceLanguage", "en")
                        .queryParam("safeSearch", "strict")
                        .queryParam("videoDuration", "medium")
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

        Map<String, Object> first = pickMostRelevant(items, query);
        if (first == null) {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            return new VideoResult(
                    "Search YouTube: " + query,
                    null,
                    null,
                    "https://www.youtube.com/results?search_query=" + encoded
            );
        }
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> pickMostRelevant(List<Map<String, Object>> items, String query) {
        Set<String> queryTokens = Arrays.stream(query.toLowerCase().split("\\s+"))
                .map(token -> token.replaceAll("[^a-z0-9]", ""))
                .filter(token -> token.length() > 2)
                .collect(Collectors.toSet());

        List<String> ids = items.stream()
                .map(item -> (Map<String, Object>) item.get("id"))
                .filter(map -> map != null && map.get("videoId") != null)
                .map(map -> String.valueOf(map.get("videoId")))
                .toList();
        if (ids.isEmpty()) {
            return items.get(0);
        }

        Map<String, Object> detailResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/videos")
                        .queryParam("part", "snippet,statistics,contentDetails")
                        .queryParam("id", String.join(",", ids))
                        .queryParam("key", properties.youtubeApiKey())
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> detailItems = Optional.ofNullable((List<Map<String, Object>>) detailResponse.get("items"))
                .orElse(List.of());
        Map<String, Map<String, Object>> detailById = detailItems.stream()
                .filter(item -> item.get("id") != null)
                .collect(Collectors.toMap(item -> String.valueOf(item.get("id")), item -> item, (a, b) -> a));

        Map<String, Object> best = items.stream()
                .max(Comparator.comparingDouble(item -> scoreItem(item, detailById, queryTokens)))
                .orElse(null);
        if (best == null || scoreItem(best, detailById, queryTokens) < 18.0) {
            return null;
        }
        return best;
    }

    @SuppressWarnings("unchecked")
    private double scoreItem(Map<String, Object> item, Map<String, Map<String, Object>> detailById, Set<String> queryTokens) {
        Map<String, Object> id = (Map<String, Object>) item.get("id");
        String videoId = id == null ? null : String.valueOf(id.get("videoId"));
        Map<String, Object> details = videoId == null ? null : detailById.get(videoId);
        Map<String, Object> snippet = details == null ? (Map<String, Object>) item.get("snippet") : (Map<String, Object>) details.get("snippet");
        Map<String, Object> stats = details == null ? null : (Map<String, Object>) details.get("statistics");
        Map<String, Object> contentDetails = details == null ? null : (Map<String, Object>) details.get("contentDetails");

        String title = snippet == null ? "" : String.valueOf(snippet.getOrDefault("title", ""));
        String lower = title.toLowerCase();

        int tokenMatches = 0;
        for (String token : queryTokens) {
            if (lower.contains(token)) {
                tokenMatches++;
            }
        }
        long views = parseLong(stats == null ? null : stats.get("viewCount"));
        long likes = parseLong(stats == null ? null : stats.get("likeCount"));

        String durationText = contentDetails == null ? null : String.valueOf(contentDetails.get("duration"));
        long seconds = parseDurationSeconds(durationText);

        double score = tokenMatches * 20.0;
        if (lower.contains("tutorial")) score += 8;
        if (lower.contains("course")) score += 6;
        if (lower.contains("short")) score -= 30;
        if (seconds > 0 && seconds < 180) score -= 40;
        if (seconds >= 300) score += 8;

        score += Math.log10(views + 1) * 6.5;
        score += Math.log10(likes + 1) * 3.5;
        return score;
    }

    private long parseLong(Object value) {
        try {
            return value == null ? 0L : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private long parseDurationSeconds(String durationValue) {
        if (!StringUtils.hasText(durationValue)) {
            return 0L;
        }
        try {
            return Duration.parse(durationValue).toSeconds();
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
