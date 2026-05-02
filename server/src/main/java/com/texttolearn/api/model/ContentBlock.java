package com.texttolearn.api.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentBlock {
    private String type;
    private String text;
    private String language;
    private String query;
    private String question;
    private List<String> options;
    private Integer answer;
    private String explanation;
}

