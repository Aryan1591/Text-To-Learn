package com.texttolearn.api.model;

import java.time.Instant;
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
public class Lesson {
    private String id;
    private String title;
    @Default
    private List<String> objectives = new ArrayList<>();
    @Default
    private List<ContentBlock> content = new ArrayList<>();
    private boolean enriched;
    private Instant createdAt;
}
