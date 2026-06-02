package com.texttolearn.api.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.texttolearn.api.dto.GenerateCourseRequest;
import com.texttolearn.api.model.ContentBlock;
import com.texttolearn.api.model.Course;
import com.texttolearn.api.model.CourseModule;
import com.texttolearn.api.model.Lesson;

@Service
public class CourseGenerationService {
    public Course generate(GenerateCourseRequest request, String creator) {
        String topic = cleanTopic(request.topic());
        String level = StringUtils.hasText(request.learningLevel()) ? request.learningLevel() : "Beginner to intermediate";
        String language = StringUtils.hasText(request.languagePreference()) ? request.languagePreference() : "English with optional Hinglish explanation";

        Instant now = Instant.now();
        return Course.builder()
                .title("Mastering " + titleCase(topic))
                .description("A practical, structured course for learning " + topic + " from fundamentals to applied projects. Designed for " + level + " learners with " + language + " support.")
                .prompt(topic)
                .creator(creator)
                .tags(tagsFor(topic))
                .modules(buildModules(topic, normalizedLevel(level)))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private List<CourseModule> buildModules(String topic, String level) {
        List<String> moduleTitles = List.of(
                "Foundations of " + titleCase(topic),
                "Core Concepts and Mental Models",
                "Hands-On Practice",
                "Real-World Applications",
                "Assessment and Next Steps"
        );

        List<CourseModule> modules = new ArrayList<>();
        for (int i = 0; i < moduleTitles.size(); i++) {
            modules.add(CourseModule.builder()
                    .id(UUID.randomUUID().toString())
                    .title(moduleTitles.get(i))
                    .summary("Module " + (i + 1) + " builds " + level.toLowerCase(Locale.ROOT) + " confidence through focused lessons and checks for understanding.")
                    .lessons(buildLessons(topic, moduleTitles.get(i), i, level))
                    .build());
        }
        return modules;
    }

    private List<Lesson> buildLessons(String topic, String moduleTitle, int moduleIndex, String level) {
        List<String> lessonSeeds = switch (moduleIndex) {
            case 0 -> List.of("What You Will Learn", "Essential Vocabulary", "How the Field Is Used");
            case 1 -> List.of("Key Principles", "Common Patterns", "Common Pitfalls and Fixes");
            case 2 -> List.of("Guided Mini Project", "Practice Workflow", "Debugging and Review");
            case 3 -> List.of("Industry Use Cases", "Tools and Resources", "Ethics and Limitations");
            default -> List.of("Knowledge Check", "Capstone Plan", "Where to Go Next");
        };

        return lessonSeeds.stream()
                .map(seed -> lesson(topic, moduleTitle, seed, level))
                .toList();
    }

    private Lesson lesson(String topic, String moduleTitle, String seed, String level) {
        String title = seed + ": " + titleCase(topic);
        List<ContentBlock> content = new ArrayList<>();
        content.add(ContentBlock.builder().type("heading").text(title).build());
        content.add(ContentBlock.builder().type("paragraph").text("This lesson connects " + moduleTitle + " to a practical understanding of " + topic + ". Start by building intuition, then attach terms and examples after the idea feels familiar.").build());
        content.add(ContentBlock.builder().type("paragraph").text("Hinglish explanation: Is topic ko step-by-step samjho. Pehle basic idea clear karo, phir examples ke through use real life mein connect karo.").build());
        ContentBlock codeBlock = codeBlockIfUseful(topic, level);
        if (codeBlock != null) {
            content.add(codeBlock);
        }
        content.add(ContentBlock.builder().type("video").query(videoQuery(topic, moduleTitle, seed, level)).build());
        content.addAll(mcqs(topic, seed, level));

        return Lesson.builder()
                .id(UUID.randomUUID().toString())
                .title(title)
                .objectives(List.of(
                        "Explain the role of " + seed.toLowerCase(Locale.ROOT) + " in " + topic + ".",
                        "Identify practical examples that make the concept easier to remember.",
                        "Apply the lesson through a small activity or reflection."
                ))
                .content(content)
                .enriched(true)
                .createdAt(Instant.now())
                .build();
    }

    private ContentBlock codeBlockIfUseful(String topic, String level) {
        String lower = topic.toLowerCase(Locale.ROOT);
        if (!(lower.contains("java") || lower.contains("react") || lower.contains("python") || lower.contains("javascript") || lower.contains("programming"))) {
            return null;
        }
        String snippet = switch (level) {
            case "advanced" -> "function solve(input) {\n  return input\n    .map((item) => transform(item))\n    .filter(Boolean)\n    .reduce((acc, curr) => acc + curr.score, 0);\n}";
            case "intermediate" -> "const result = items\n  .filter((item) => item.active)\n  .map((item) => item.name.toUpperCase());\nconsole.log(result);";
            default -> "console.log('Start with a tiny working example and iterate.');";
        };
        return ContentBlock.builder()
                .type("code")
                .language(lower.contains("python") ? "python" : lower.contains("java") ? "java" : "javascript")
                .text(lower.contains("java")
                        ? "public class Practice {\n    public static void main(String[] args) {\n        System.out.println(\"Build, test, and refine in small steps.\");\n    }\n}"
                        : lower.contains("python")
                        ? "def practice(items):\n    active = [item for item in items if item.get('active')]\n    return len(active)\n\nprint(practice([{'active': True}, {'active': False}]))"
                        : snippet)
                .build();
    }

    private List<ContentBlock> mcqs(String topic, String seed, String level) {
        String difficultyHint = switch (level) {
            case "advanced" -> "in advanced scenarios";
            case "intermediate" -> "when moving from basics to practical use";
            default -> "as a beginner";
        };
        return List.of(
                ContentBlock.builder()
                        .type("mcq")
                        .question("Which habit best supports learning " + seed.toLowerCase(Locale.ROOT) + " in " + topic + " " + difficultyHint + "?")
                        .options(List.of(
                                "Memorize definitions without practice",
                                "Connect concepts to examples and self-test",
                                "Skip fundamentals and jump to tools",
                                "Only watch videos and avoid exercises"
                        ))
                        .answer(1)
                        .explanation("Active recall plus examples improves retention and transfer.")
                        .build(),
                ContentBlock.builder()
                        .type("mcq")
                        .question("What is the best first step when a concept in " + topic + " feels confusing?")
                        .options(List.of(
                                "Ignore it and continue",
                                "Break it into smaller parts and restate in your own words",
                                "Copy solutions without understanding",
                                "Switch topics immediately"
                        ))
                        .answer(1)
                        .explanation("Decomposing and rephrasing creates a stronger mental model.")
                        .build(),
                ContentBlock.builder()
                        .type("mcq")
                        .question("How should you use mistakes while practicing " + seed.toLowerCase(Locale.ROOT) + "?")
                        .options(List.of(
                                "Hide mistakes and move on quickly",
                                "Track mistakes and identify recurring patterns",
                                "Practice only what you already know",
                                "Avoid feedback from quizzes"
                        ))
                        .answer(1)
                        .explanation("Pattern-based review turns mistakes into reusable learning signals.")
                        .build(),
                ContentBlock.builder()
                        .type("mcq")
                        .question("Which revision strategy is most effective for " + topic + " at " + level + " level?")
                        .options(List.of(
                                "One long session once a month",
                                "Short spaced reviews plus one mini-application task",
                                "Only passive reading",
                                "Skip revision entirely"
                        ))
                        .answer(1)
                        .explanation("Spacing and application are consistently strong learning strategies.")
                        .build()
        );
    }

    private String videoQuery(String topic, String moduleTitle, String seed, String level) {
        String levelTag = switch (level) {
            case "advanced" -> "advanced deep dive";
            case "intermediate" -> "intermediate tutorial";
            default -> "beginner tutorial";
        };
        return topic + " " + moduleTitle + " " + seed + " " + levelTag;
    }

    private String normalizedLevel(String level) {
        String lower = level == null ? "" : level.toLowerCase(Locale.ROOT);
        if (lower.contains("advanced")) {
            return "advanced";
        }
        if (lower.contains("intermediate")) {
            return "intermediate";
        }
        return "beginner";
    }

    private String cleanTopic(String topic) {
        String cleaned = topic == null ? "" : topic.trim().replaceAll("\\s+", " ");
        if (cleaned.length() < 3) {
            throw new IllegalArgumentException("Please enter a more specific topic.");
        }
        return cleaned;
    }

    private List<String> tagsFor(String topic) {
        return List.of("ai-generated", "self-learning", topic.toLowerCase(Locale.ROOT).replace(" ", "-"));
    }

    private String titleCase(String value) {
        String[] words = value.toLowerCase(Locale.ROOT).split(" ");
        List<String> result = new ArrayList<>();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            result.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
        }
        return String.join(" ", result);
    }
}
