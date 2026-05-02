package com.texttolearn.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.texttolearn.api.dto.VideoResult;
import com.texttolearn.api.service.YouTubeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YouTubeController {
    private final YouTubeService youTubeService;

    @GetMapping
    VideoResult search(@RequestParam String query) {
        return youTubeService.search(query);
    }
}

