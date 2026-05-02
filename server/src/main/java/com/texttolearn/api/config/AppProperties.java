package com.texttolearn.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String clientOrigin,
        String auth0Audience,
        String geminiApiKey,
        String geminiModel,
        String openaiApiKey,
        String youtubeApiKey
) {
}
