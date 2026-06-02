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
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
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
        log.info("Requesting Gemini course generation for topic: '{}', level: '{}', model: '{}'",
                request.topic(), request.learningLevel(), properties.geminiModel());
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
        log.debug("Raw Gemini JSON response: {}", json);
        try {
            String stripped = stripCodeFence(json);
            Course course = objectMapper.readValue(stripped, Course.class);
            hydrateGeneratedCourse(course, request.topic(), creator);
            log.info("Successfully generated course with {} modules from Gemini",
                    course.getModules() != null ? course.getModules().size() : 0);
            return course;
        } catch (Exception ex) {
            log.error("Failed to parse Gemini JSON. Raw JSON:\n{}\nError: {}", json, ex.getMessage(), ex);
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
                - Every module title and lesson title must be explicitly tied to the input topic. Avoid generic titles that could fit any subject.
                - For technical/engineering topics (like "System Design for Uber"), focus the lesson content and titles on actual concrete architectural components, data flows, databases, design choices, trade-offs, and protocols (e.g. Geospatial indexing, quadtrees, web sockets, MongoDB vs Cassandra, consistent hashing, matching algorithms) instead of generic high-level tutorials.
                - Each lesson must include objectives, at least 2 paragraphs, a video query, and 4 MCQs.
                - The 4 MCQs in a lesson must be unique and test different ideas (concept, application, mistake analysis, revision strategy).
                - Add code blocks only for programming/technical topics where code is useful.
                - Adapt depth, terminology, examples, and MCQ difficulty to learner level.
                - Video query should include topic + lesson title + learner level keyword (beginner/intermediate/advanced).
                - Use zero-based index for MCQ answer.
                - Keep content accurate, beginner-friendly, and practical.
                - Do not use filler headings like "Mistakes Beginners Make" unless followed by a topic-specific qualifier.
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
