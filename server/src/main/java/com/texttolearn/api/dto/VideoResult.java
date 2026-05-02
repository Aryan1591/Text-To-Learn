package com.texttolearn.api.dto;

public record VideoResult(
        String title,
        String videoId,
        String embedUrl,
        String sourceUrl
) {
}

