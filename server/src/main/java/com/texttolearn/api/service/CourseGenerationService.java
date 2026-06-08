package com.texttolearn.api.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.texttolearn.api.dto.GenerateCourseRequest;
import com.texttolearn.api.model.ContentBlock;
import com.texttolearn.api.model.Course;
import com.texttolearn.api.model.CourseModule;
import com.texttolearn.api.model.Lesson;

@Service
public class CourseGenerationService {
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9\\s]");

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
        int moduleCount = estimateModuleCount(topic);
        List<String> moduleTitles = buildModuleTitles(topic, moduleCount);
        List<CourseModule> modules = new ArrayList<>();

        for (int i = 0; i < moduleTitles.size(); i++) {
            String moduleTitle = moduleTitles.get(i);
            modules.add(CourseModule.builder()
                    .id(UUID.randomUUID().toString())
                    .title(moduleTitle)
                    .summary(moduleSummary(topic, i, level))
                    .lessons(buildLessons(topic, moduleTitle, i, level))
                    .build());
        }
        return modules;
    }

    private List<String> buildModuleTitles(String topic, int moduleCount) {
        List<String> baseTitles = List.of(
                "Foundations of " + titleCase(topic),
                "Core Concepts of " + titleCase(topic),
                "Applied Use of " + titleCase(topic),
                "Common Pitfalls in " + titleCase(topic),
                "Review and Next Steps for " + titleCase(topic)
        );
        return new ArrayList<>(baseTitles.subList(0, moduleCount));
    }

    private List<Lesson> buildLessons(String topic, String moduleTitle, int moduleIndex, String level) {
        List<String> lessonSeeds = switch (moduleIndex) {
            case 0 -> List.of("What It Covers", "Core Terms", "Why It Matters");
            case 1 -> List.of("How It Works", "Main Patterns", "Common Mistakes");
            case 2 -> List.of("Practical Examples", "Step-by-Step Workflow", "Decision Points");
            case 3 -> List.of("Constraints", "Trade-offs", "Failure Cases");
            default -> List.of("Review", "Applied Challenge", "Next Steps");
        };

        List<Lesson> lessons = new ArrayList<>();
        for (int i = 0; i < lessonSeeds.size(); i++) {
            lessons.add(buildLesson(topic, moduleTitle, lessonSeeds.get(i), level, moduleIndex, i));
        }
        return lessons;
    }

    private Lesson buildLesson(String topic, String moduleTitle, String seed, String level, int moduleIndex, int lessonIndex) {
        String title = seed + ": " + titleCase(topic);
        List<ContentBlock> content = new ArrayList<>();
        content.add(ContentBlock.builder().type("heading").text(title).build());
        content.add(ContentBlock.builder().type("paragraph").text(primaryParagraph(topic, moduleTitle, seed, moduleIndex, lessonIndex)).build());
        content.add(ContentBlock.builder().type("paragraph").text(secondaryParagraph(topic, level, moduleIndex, lessonIndex)).build());

        ContentBlock codeBlock = codeBlockIfUseful(topic, level);
        if (codeBlock != null) {
            content.add(codeBlock);
        }

        content.add(ContentBlock.builder().type("video").query(videoQuery(topic, moduleTitle, seed, level)).build());
        content.addAll(mcqs(topic, seed, level, moduleIndex, lessonIndex));

        return Lesson.builder()
                .id(UUID.randomUUID().toString())
                .title(title)
                .objectives(List.of(
                        objectiveOne(topic, seed),
                        objectiveTwo(topic, seed),
                        objectiveThree(topic, seed)
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

    private List<ContentBlock> mcqs(String topic, String seed, String level, int moduleIndex, int lessonIndex) {
        String difficultyHint = switch (level) {
            case "advanced" -> "in advanced scenarios";
            case "intermediate" -> "when moving from basics to practical use";
            default -> "as a beginner";
        };
        String focus = titleCase(seed + " in " + topic);
        String[] q1 = {
                "Which statement best captures the purpose of " + focus + " " + difficultyHint + "?",
                "What is the most accurate reason to study " + focus + " " + difficultyHint + "?",
                "Which idea should you understand first in " + focus + " " + difficultyHint + "?"
        };
        String[] q2 = {
                "When would " + focus + " matter most in a real scenario?",
                "What practical situation would make " + focus + " especially useful?",
                "Which workflow step depends on understanding " + focus + "?"
        };
        String[] q3 = {
                "What is a common mistake when applying " + focus + "?",
                "Which error would most likely cause confusion when using " + focus + "?",
                "What should you avoid when trying to remember " + focus + "?"
        };
        String[] q4 = {
                "Which review method is best after learning " + focus + "?",
                "What is the strongest way to check your understanding of " + focus + "?",
                "Which practice habit helps you retain " + focus + " longer?"
        };

        int variant = (moduleIndex + lessonIndex) % q1.length;
        String topicFocus = titleCase(topic);
        return List.of(
                mcq(q1[variant], List.of(
                        "Memorize definitions without practice",
                        "Connect the idea to examples and self-test",
                        "Skip fundamentals and jump straight to tools",
                        "Only watch videos and avoid exercises"
                ), 1, "Active recall plus examples improves retention and transfer."),
                mcq(q2[variant], List.of(
                        "A random unrelated topic",
                        "A concrete real-world use case",
                        "A shortcut that avoids understanding",
                        "A memory trick with no application"
                ), 1, "Applying the concept to a real scenario makes it stick."),
                mcq(q3[variant], List.of(
                        "Leaving the idea vague and moving on",
                        "Breaking it into smaller parts and restating it clearly",
                        "Copying a solution without understanding",
                        "Ignoring feedback and repetition"
                ), 1, "Decomposing and rephrasing builds a stronger mental model."),
                mcq(q4[variant], List.of(
                        "One long session once a month",
                        "Short spaced reviews plus one mini-application task",
                        "Only passive reading",
                        "Skipping revision entirely"
                ), 1, "Spacing and application are consistently strong learning strategies.")
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

    private String moduleSummary(String topic, int index, String level) {
        String[] summaryStyles = {
                "sets up the foundations and key terms",
                "connects the main ideas into a working mental model",
                "moves into practical application and examples",
                "surfaces common mistakes, trade-offs, and constraints",
                "ties everything together with review and practice"
        };
        return "Module " + (index + 1) + " " + summaryStyles[Math.min(index, summaryStyles.length - 1)] +
                " for " + topic + " at " + level.toLowerCase(Locale.ROOT) + " depth.";
    }

    private String primaryParagraph(String topic, String moduleTitle, String seed, int moduleIndex, int lessonIndex) {
        String[] angles = {
                "the main idea",
                "the moving parts",
                "a concrete example",
                "the decision points",
                "the failure modes"
        };
        String angle = angles[(moduleIndex + lessonIndex) % angles.length];
        return "This lesson uses " + moduleTitle + " to explain " + seed.toLowerCase(Locale.ROOT) + " in " + topic + " from the angle of " + angle + ". Lesson " + (moduleIndex + 1) + "." + (lessonIndex + 1) + " should stay anchored to the exact prompt and avoid generic filler.";
    }

    private String secondaryParagraph(String topic, String level, int moduleIndex, int lessonIndex) {
        String[] practiceAngles = {
                "ask what problem it solves",
                "compare two possible approaches",
                "spot a likely mistake",
                "explain it in one minute",
                "apply it to a fresh scenario"
        };
        String practice = practiceAngles[(moduleIndex * 3 + lessonIndex) % practiceAngles.length];
        return "For " + level.toLowerCase(Locale.ROOT) + " learners, use this part to " + practice + " while keeping the topic anchored to " + topic + ". This checkpoint is specific to lesson " + (moduleIndex + 1) + "." + (lessonIndex + 1) + ".";
    }

    private String objectiveOne(String topic, String seed) {
        return "Explain the role of " + seed.toLowerCase(Locale.ROOT) + " in " + topic + ".";
    }

    private String objectiveTwo(String topic, String seed) {
        return "Identify practical examples that make " + seed.toLowerCase(Locale.ROOT) + " easier to remember in " + topic + ".";
    }

    private String objectiveThree(String topic, String seed) {
        return "Apply the lesson through a small activity or reflection for " + topic + ".";
    }

    private int estimateModuleCount(String topic) {
        String normalized = topic.toLowerCase(Locale.ROOT).trim();
        int wordCount = normalized.isEmpty() ? 1 : normalized.split("\\s+").length;
        if (wordCount <= 2) {
            return 3;
        }
        if (wordCount <= 4) {
            return 4;
        }
        return 5;
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
}
