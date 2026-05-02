package com.texttolearn.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "courses")
public class Course {
    @Id
    private String id;
    private String title;
    private String description;
    private String prompt;
    private String creator;
    @Default
    private List<String> tags = new ArrayList<>();
    @Default
    private List<CourseModule> modules = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
