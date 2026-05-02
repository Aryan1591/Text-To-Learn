package com.texttolearn.api.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseModule {
    private String id;
    private String title;
    private String summary;
    @Default
    private List<Lesson> lessons = new ArrayList<>();
}
