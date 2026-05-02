package com.texttolearn.api.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.texttolearn.api.dto.GenerateCourseRequest;
import com.texttolearn.api.model.Course;
import com.texttolearn.api.security.CurrentUser;
import com.texttolearn.api.service.CourseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;
    private final CurrentUser currentUser;

    @PostMapping("/generate")
    Course generate(@Valid @RequestBody GenerateCourseRequest request, Authentication authentication) {
        return courseService.generateAndSave(request, currentUser.subject(authentication));
    }

    @GetMapping
    List<Course> recent() {
        return courseService.recentCourses();
    }

    @GetMapping("/my")
    List<Course> mine(Authentication authentication) {
        return courseService.userCourses(currentUser.subject(authentication));
    }

    @GetMapping("/{id}")
    Course byId(@PathVariable String id) {
        return courseService.getById(id);
    }

    @DeleteMapping("/{id}")
    void delete(@PathVariable String id, Authentication authentication) {
        courseService.delete(id, currentUser.subject(authentication));
    }
}

