package com.texttolearn.api.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.texttolearn.api.model.Course;

public interface CourseRepository extends MongoRepository<Course, String> {
    List<Course> findTop20ByOrderByCreatedAtDesc();
    List<Course> findByCreatorOrderByCreatedAtDesc(String creator);
}

