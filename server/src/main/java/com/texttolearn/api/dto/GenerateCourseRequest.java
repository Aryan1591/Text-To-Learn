package com.texttolearn.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateCourseRequest(
        @NotBlank(message = "Topic is required")
        @Size(min = 3, max = 160, message = "Topic must be between 3 and 160 characters")
        String topic,
        String learningLevel,
        String languagePreference
) {
}

