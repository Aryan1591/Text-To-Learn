package com.texttolearn.api.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.texttolearn.api.dto.GenerateCourseRequest;
import com.texttolearn.api.exception.ResourceNotFoundException;
import com.texttolearn.api.model.ContentBlock;
import com.texttolearn.api.model.Course;
import com.texttolearn.api.model.CourseModule;
import com.texttolearn.api.model.Lesson;
import com.texttolearn.api.repository.CourseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9\\s]");
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "or", "the", "to", "for", "of", "in", "on", "with", "from",
            "is", "are", "be", "by", "as", "at", "into", "about", "over", "under"
    );

    private final CourseRepository courseRepository;
    private final CourseGenerationService generationService;
    private final GeminiCourseGenerationService geminiCourseGenerationService;

    public Course generateAndSave(GenerateCourseRequest request, String creator) {
        String topic = request.topic();
        Course generated;
        if (geminiCourseGenerationService.isConfigured()) {
            try {
                generated = geminiCourseGenerationService.generate(request, creator);
            } catch (RuntimeException ex) {
                generated = generationService.generate(request, creator);
            }
        } else {
            generated = generationService.generate(request, creator);
        }

        // Guardrail: if AI drifts off-topic, fallback to deterministic topic-anchored generator.
        if (!isTopicallyRelevant(generated, topic)) {
            generated = generationService.generate(request, creator);
        }

        normalizeCourse(generated, request.learningLevel(), topic);
        generated.setCreatedAt(Instant.now());
        generated.setUpdatedAt(Instant.now());
        return courseRepository.save(generated);
    }

    public List<Course> recentCourses() {
        return courseRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public List<Course> userCourses(String creator) {
        return courseRepository.findByCreatorOrderByCreatedAtDesc(creator);
    }

    public Course getById(String id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    public void delete(String id, String creator) {
        Course course = getById(id);
        if (!creator.equals(course.getCreator())) {
            throw new AccessDeniedException("You can only delete your own courses.");
        }
        courseRepository.delete(course);
    }

    private void normalizeCourse(Course course, String learningLevel, String topic) {
        if (course == null || course.getModules() == null) {
            return;
        }
        String level = normalizeLevel(learningLevel);
        String topicText = StringUtils.hasText(topic) ? topic.trim() : "the topic";
        for (CourseModule module : course.getModules()) {
            if (!StringUtils.hasText(module.getSummary())) {
                module.setSummary("Focused coverage of " + topicText + " with practical progression and revision checkpoints.");
            }
            if (module.getLessons() == null) {
                continue;
            }
            for (Lesson lesson : module.getLessons()) {
                if (!containsTopicHint(lesson.getTitle(), topicText)) {
                    lesson.setTitle(lesson.getTitle() + " - " + topicText);
                }
                if (lesson.getContent() == null) {
                    continue;
                }
                List<ContentBlock> normalized = new ArrayList<>();
                Set<String> seenMcqQuestions = new LinkedHashSet<>();
                Set<String> seenTextBlocks = new LinkedHashSet<>();
                boolean videoAdded = false;

                for (ContentBlock block : lesson.getContent()) {
                    if (block == null) {
                        continue;
                    }
                    if ("mcq".equalsIgnoreCase(block.getType())) {
                        String questionKey = StringUtils.hasText(block.getQuestion())
                                ? block.getQuestion().trim().toLowerCase(Locale.ROOT)
                                : "";
                        if (!StringUtils.hasText(questionKey) || seenMcqQuestions.contains(questionKey)) {
                            continue;
                        }
                        seenMcqQuestions.add(questionKey);
                        normalized.add(block);
                        continue;
                    }
                    if ("heading".equalsIgnoreCase(block.getType()) || "paragraph".equalsIgnoreCase(block.getType())) {
                        String textKey = StringUtils.hasText(block.getText())
                                ? block.getType().toLowerCase(Locale.ROOT) + "|" + block.getText().trim().toLowerCase(Locale.ROOT)
                                : "";
                        if (StringUtils.hasText(textKey) && seenTextBlocks.contains(textKey)) {
                            continue;
                        }
                        if (StringUtils.hasText(textKey)) {
                            seenTextBlocks.add(textKey);
                        }
                    }
                    if ("video".equalsIgnoreCase(block.getType())) {
                        if (videoAdded) {
                            continue;
                        }
                        block.setQuery(buildVideoQuery(course.getTitle(), module.getTitle(), lesson.getTitle(), level));
                        videoAdded = true;
                    }
                    normalized.add(block);
                }

                // Guarantee at least one strong video block.
                if (!videoAdded) {
                    normalized.add(ContentBlock.builder()
                            .type("video")
                            .query(buildVideoQuery(course.getTitle(), module.getTitle(), lesson.getTitle(), level))
                            .build());
                }

                // Guarantee 4 distinct MCQs even if AI response is noisy.
                List<ContentBlock> mcqs = normalized.stream()
                        .filter(block -> "mcq".equalsIgnoreCase(block.getType()))
                        .toList();
                if (mcqs.size() < 4) {
                    List<ContentBlock> fillers = buildFallbackMcqs(course.getTitle(), lesson.getTitle(), level);
                    for (ContentBlock filler : fillers) {
                        String key = filler.getQuestion().trim().toLowerCase(Locale.ROOT);
                        if (!seenMcqQuestions.contains(key)) {
                            normalized.add(filler);
                            seenMcqQuestions.add(key);
                        }
                        if (seenMcqQuestions.size() >= 4) {
                            break;
                        }
                    }
                }
                lesson.setContent(normalized);
            }
        }
    }

    private String normalizeLevel(String level) {
        String lower = level == null ? "" : level.toLowerCase(Locale.ROOT);
        if (lower.contains("advanced")) {
            return "advanced";
        }
        if (lower.contains("intermediate")) {
            return "intermediate";
        }
        return "beginner";
    }

    private String buildVideoQuery(String courseTitle, String moduleTitle, String lessonTitle, String level) {
        String levelHint = switch (level) {
            case "advanced" -> "advanced deep dive";
            case "intermediate" -> "intermediate tutorial";
            default -> "beginner tutorial";
        };
        return courseTitle + " " + moduleTitle + " " + lessonTitle + " " + levelHint;
    }

    private List<ContentBlock> buildFallbackMcqs(String topic, String lessonTitle, String level) {
        return List.of(
                mcq("Which statement best summarizes " + lessonTitle + " in " + topic + "?", List.of(
                        "It is only theory with no practical value",
                        "It links core concepts to practical application",
                        "It should be skipped at " + level + " level",
                        "It is only useful for memorization"
                ), 1, "This lesson should connect concept and execution."),
                mcq("What is the most effective way to practice this lesson?", List.of(
                        "Read once and move on",
                        "Build one small example and explain it in your own words",
                        "Avoid mistakes by not attempting exercises",
                        "Copy answers without checking reasoning"
                ), 1, "Active practice + articulation is usually strongest."),
                mcq("When you get stuck, what should you do first?", List.of(
                        "Switch to a random new topic",
                        "Break the problem into smaller steps and validate assumptions",
                        "Ignore the issue and continue",
                        "Only watch another video without practice"
                ), 1, "Structured debugging helps across all levels."),
                mcq("How should revision change with difficulty level?", List.of(
                        "Keep the same shallow strategy for all levels",
                        "Increase depth: explain why, compare tradeoffs, and apply in new contexts",
                        "Skip revision once basics are done",
                        "Rely only on memory without retrieval practice"
                ), 1, "Higher levels require deeper transfer and reasoning.")
        );
    }

    private ContentBlock mcq(String question, List<String> options, int answer, String explanation) {
        return ContentBlock.builder()
                .type("mcq")
                .question(question)
                .options(options)
                .answer(answer)
                .explanation(explanation)
                .build();
    }

    private boolean containsTopicHint(String text, String topic) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(topic)) {
            return false;
        }
        String lhs = text.toLowerCase(Locale.ROOT);
        String rhs = topic.toLowerCase(Locale.ROOT);
        if (lhs.contains(rhs)) {
            return true;
        }
        for (String token : rhs.split("\\s+")) {
            if (token.length() > 3 && lhs.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTopicallyRelevant(Course course, String topic) {
        if (course == null || !StringUtils.hasText(topic) || course.getModules() == null || course.getModules().isEmpty()) {
            log.warn("Relevance Check: Course or modules are empty/null for topic '{}'", topic);
            return false;
        }
        Set<String> topicTokens = tokenize(topic);
        if (topicTokens.isEmpty()) {
            log.warn("Relevance Check: Tokenized topic is empty for '{}'", topic);
            return false;
        }

        // Primary metadata match (Title / Description)
        boolean titleMatches = matchesTopic(course.getTitle(), topic, topicTokens);
        boolean descriptionMatches = matchesTopic(course.getDescription(), topic, topicTokens);
        boolean primaryMetadataMatches = titleMatches || descriptionMatches;

        int totalUnits = 0;
        int matchedUnits = 0;

        for (CourseModule module : course.getModules()) {
            if (module == null) {
                continue;
            }
            totalUnits++;
            boolean moduleMatched = matchesTopic(module.getTitle(), topic, topicTokens)
                    || matchesTopic(module.getSummary(), topic, topicTokens);
            if (moduleMatched) {
                matchedUnits++;
            }
            if (module.getLessons() == null) {
                continue;
            }
            for (Lesson lesson : module.getLessons()) {
                if (lesson == null) {
                    continue;
                }
                totalUnits++;
                boolean lessonMatched = matchesTopic(lesson.getTitle(), topic, topicTokens);
                if (!lessonMatched && lesson.getObjectives() != null) {
                    for (String objective : lesson.getObjectives()) {
                        if (matchesTopic(objective, topic, topicTokens)) {
                            lessonMatched = true;
                            break;
                        }
                    }
                }
                if (lessonMatched) {
                    matchedUnits++;
                }
            }
        }

        if (totalUnits == 0) {
            log.warn("Relevance Check: Calculated 0 total units for topic '{}'", topic);
            return false;
        }
        double ratio = (double) matchedUnits / totalUnits;
        double requiredRatio = primaryMetadataMatches ? 0.15 : 0.35;
        boolean isRelevant = ratio >= requiredRatio;

        log.info("Relevance Check for '{}': primaryMetadataMatches={}, titleMatches={}, descriptionMatches={}, " +
                "matchedUnits={}, totalUnits={}, ratio={}, requiredRatio={}, outcome={}",
                topic, primaryMetadataMatches, titleMatches, descriptionMatches,
                matchedUnits, totalUnits, ratio, requiredRatio, isRelevant ? "PASSED" : "FAILED (FALLING BACK)");

        return isRelevant;
    }

    private boolean matchesTopic(String text, String topic, Set<String> topicTokens) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = normalizeText(text);
        String normalizedTopic = normalizeText(topic);
        if (normalized.contains(normalizedTopic)) {
            return true;
        }
        Set<String> textTokens = tokenize(text);
        if (textTokens.isEmpty()) {
            return false;
        }
        int overlap = 0;
        for (String token : topicTokens) {
            if (textTokens.contains(token)) {
                overlap++;
            }
        }
        return overlap >= Math.max(1, topicTokens.size() / 2);
    }

    private Set<String> tokenize(String text) {
        String normalized = normalizeText(text);
        Set<String> tokens = new HashSet<>();
        for (String part : normalized.split("\\s+")) {
            if (part.length() < 3 || STOP_WORDS.contains(part)) {
                continue;
            }
            tokens.add(part);
        }
        return tokens;
    }

    private String normalizeText(String text) {
        return NON_ALNUM.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ").trim().replaceAll("\\s+", " ");
    }
}
