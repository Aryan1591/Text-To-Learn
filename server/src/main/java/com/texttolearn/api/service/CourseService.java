package com.texttolearn.api.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.texttolearn.api.dto.GenerateCourseRequest;
import com.texttolearn.api.exception.ResourceNotFoundException;
import com.texttolearn.api.model.Course;
import com.texttolearn.api.repository.CourseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseGenerationService generationService;
    private final GeminiCourseGenerationService geminiCourseGenerationService;

    public Course generateAndSave(GenerateCourseRequest request, String creator) {
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
}
