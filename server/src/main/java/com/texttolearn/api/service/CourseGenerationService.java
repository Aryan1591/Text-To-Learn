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
                .modules(buildModules(topic, level))
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
                    .lessons(buildLessons(topic, moduleTitles.get(i), i))
                    .build());
        }
        return modules;
    }

    private List<Lesson> buildLessons(String topic, String moduleTitle, int moduleIndex) {
        List<String> lessonSeeds = switch (moduleIndex) {
            case 0 -> List.of("What You Will Learn", "Essential Vocabulary", "How the Field Is Used");
            case 1 -> List.of("Key Principles", "Common Patterns", "Mistakes Beginners Make");
            case 2 -> List.of("Guided Mini Project", "Practice Workflow", "Debugging and Review");
            case 3 -> List.of("Industry Use Cases", "Tools and Resources", "Ethics and Limitations");
            default -> List.of("Knowledge Check", "Capstone Plan", "Where to Go Next");
        };

        return lessonSeeds.stream()
                .map(seed -> lesson(topic, moduleTitle, seed))
                .toList();
    }

    private Lesson lesson(String topic, String moduleTitle, String seed) {
        String title = seed + ": " + titleCase(topic);
        List<ContentBlock> content = new ArrayList<>();
        content.add(ContentBlock.builder().type("heading").text(title).build());
        content.add(ContentBlock.builder().type("paragraph").text("This lesson connects " + moduleTitle + " to a practical understanding of " + topic + ". Start by building intuition, then attach terms and examples after the idea feels familiar.").build());
        content.add(ContentBlock.builder().type("paragraph").text("Hinglish explanation: Is topic ko step-by-step samjho. Pehle basic idea clear karo, phir examples ke through use real life mein connect karo.").build());
        ContentBlock codeBlock = codeBlockIfUseful(topic);
        if (codeBlock != null) {
            content.add(codeBlock);
        }
        content.add(ContentBlock.builder().type("video").query(topic + " " + seed + " beginner tutorial").build());
        content.add(mcq(topic, seed));
        content.add(mcq(topic, seed));
        content.add(mcq(topic, seed));
        content.add(mcq(topic, seed));

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

    private ContentBlock codeBlockIfUseful(String topic) {
        String lower = topic.toLowerCase(Locale.ROOT);
        if (!(lower.contains("java") || lower.contains("react") || lower.contains("python") || lower.contains("javascript") || lower.contains("programming"))) {
            return null;
        }
        return ContentBlock.builder()
                .type("code")
                .language(lower.contains("python") ? "python" : lower.contains("java") ? "java" : "javascript")
                .text(lower.contains("java") ? "public class Practice {\\n    public static void main(String[] args) {\\n        System.out.println(\"Learn by building small examples.\");\\n    }\\n}" : "console.log('Learn by building small examples.');")
                .build();
    }

    private ContentBlock mcq(String topic, String seed) {
        return ContentBlock.builder()
                .type("mcq")
                .question("Which habit best supports learning " + seed.toLowerCase(Locale.ROOT) + " in " + topic + "?")
                .options(List.of(
                        "Memorize definitions without practice",
                        "Connect the idea to examples and test yourself",
                        "Skip fundamentals and start with advanced tools",
                        "Only watch videos without taking notes"
                ))
                .answer(1)
                .explanation("Active recall plus examples helps you retain " + topic + " more reliably than passive reading.")
                .build();
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
