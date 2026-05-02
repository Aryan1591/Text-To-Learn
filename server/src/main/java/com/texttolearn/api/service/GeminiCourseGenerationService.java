package com.texttolearn.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.texttolearn.api.config.AppProperties;
import com.texttolearn.api.dto.GenerateCourseRequest;
import com.texttolearn.api.model.Course;
import com.texttolearn.api.model.CourseModule;
import com.texttolearn.api.model.Lesson;

@Service
public class GeminiCourseGenerationService {
    private final AppProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiCourseGenerationService(AppProperties properties, WebClient.Builder builder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.webClient = builder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public boolean isConfigured() {
        return StringUtils.hasText(properties.geminiApiKey());
    }

    @SuppressWarnings("unchecked")
    public Course generate(GenerateCourseRequest request, String creator) {
        String prompt = buildPrompt(request);
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.65,
                        "responseMimeType", "application/json"
                )
        );

        Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", properties.geminiApiKey())
                        .build(properties.geminiModel()))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String json = extractText(response);
        try {
            Course course = objectMapper.readValue(stripCodeFence(json), Course.class);
            hydrateGeneratedCourse(course, request.topic(), creator);
            return course;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Gemini returned invalid course JSON. Try again with a clearer topic.");
        }
    }

    private String extractText(Map<String, Object> response) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("Gemini did not return content.");
        }
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("Gemini response did not include text.");
        }
        return (String) parts.get(0).get("text");
    }

    private String buildPrompt(GenerateCourseRequest request) {
        return """
                Generate a complete online course for this topic: "%s".

                Learner level: %s
                Language preference: %s

                Return raw JSON only. Do not include Markdown fences, comments, or explanations.

                Required JSON shape:
                {
                  "title": "Course title",
                  "description": "2 sentence course description",
                  "tags": ["tag-one", "tag-two"],
                  "modules": [
                    {
                      "title": "Module title",
                      "summary": "Short summary",
                      "lessons": [
                        {
                          "title": "Lesson title",
                          "objectives": ["Objective 1", "Objective 2", "Objective 3"],
                          "enriched": true,
                          "content": [
                            { "type": "heading", "text": "Section heading" },
                            { "type": "paragraph", "text": "Clear explanation" },
                            { "type": "paragraph", "text": "Hinglish explanation: ..." },
                            { "type": "code", "language": "javascript", "text": "only if relevant" },
                            { "type": "video", "query": "educational YouTube search query" },
                            {
                              "type": "mcq",
                              "question": "Question?",
                              "options": ["A", "B", "C", "D"],
                              "answer": 1,
                              "explanation": "Why the answer is correct"
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }

                Rules:
                - Create 3 to 6 modules.
                - Each module must contain 3 to 5 lessons.
                - Each lesson must include objectives, at least 2 paragraphs, a video query, and 4 MCQs.
                - Add code blocks only for programming/technical topics where code is useful.
                - Use zero-based index for MCQ answer.
                - Keep content accurate, beginner-friendly, and practical.
                """.formatted(
                request.topic(),
                StringUtils.hasText(request.learningLevel()) ? request.learningLevel() : "Beginner to intermediate",
                StringUtils.hasText(request.languagePreference()) ? request.languagePreference() : "English with Hinglish support"
        );
    }

    private String stripCodeFence(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replaceFirst("^```json\\s*", "")
                .replaceFirst("^```\\s*", "")
                .replaceFirst("\\s*```$", "");
    }

    private void hydrateGeneratedCourse(Course course, String prompt, String creator) {
        Instant now = Instant.now();
        course.setId(null);
        course.setPrompt(prompt);
        course.setCreator(creator);
        course.setCreatedAt(now);
        course.setUpdatedAt(now);

        if (course.getModules() != null) {
            for (CourseModule module : course.getModules()) {
                module.setId(UUID.randomUUID().toString());
                if (module.getLessons() != null) {
                    for (Lesson lesson : module.getLessons()) {
                        lesson.setId(UUID.randomUUID().toString());
                        lesson.setCreatedAt(now);
                        lesson.setEnriched(true);
                    }
                }
            }
        }
    }
}

