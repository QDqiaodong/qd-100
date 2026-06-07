package com.example.shortvideo.controller;

import com.example.shortvideo.dto.response.ApiResponse;
import com.example.shortvideo.dto.response.AuthorHeatSnapshotDTO;
import com.example.shortvideo.dto.response.TagHeatSnapshotDTO;
import com.example.shortvideo.dto.response.VideoHeatSnapshotDTO;
import com.example.shortvideo.entity.AuthorHeatSnapshot;
import com.example.shortvideo.entity.TagHeatSnapshot;
import com.example.shortvideo.entity.VideoHeatSnapshot;
import com.example.shortvideo.service.HeatSnapshotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/heat-snapshots")
public class HeatSnapshotController {

    private final HeatSnapshotService heatSnapshotService;

    public HeatSnapshotController(HeatSnapshotService heatSnapshotService) {
        this.heatSnapshotService = heatSnapshotService;
    }

    @GetMapping("/videos/latest")
    public ApiResponse<List<VideoHeatSnapshotDTO>> getLatestVideoHeatSnapshots(
            @RequestParam(defaultValue = "50") int limit) {
        List<VideoHeatSnapshot> snapshots = heatSnapshotService.getLatestVideoHeatSnapshots(limit);
        List<VideoHeatSnapshotDTO> dtos = snapshots.stream()
                .map(VideoHeatSnapshotDTO::fromEntity)
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @GetMapping("/tags/latest")
    public ApiResponse<List<TagHeatSnapshotDTO>> getLatestTagHeatSnapshots(
            @RequestParam(defaultValue = "50") int limit) {
        List<TagHeatSnapshot> snapshots = heatSnapshotService.getLatestTagHeatSnapshots(limit);
        List<TagHeatSnapshotDTO> dtos = snapshots.stream()
                .map(TagHeatSnapshotDTO::fromEntity)
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @GetMapping("/authors/latest")
    public ApiResponse<List<AuthorHeatSnapshotDTO>> getLatestAuthorHeatSnapshots(
            @RequestParam(defaultValue = "50") int limit) {
        List<AuthorHeatSnapshot> snapshots = heatSnapshotService.getLatestAuthorHeatSnapshots(limit);
        List<AuthorHeatSnapshotDTO> dtos = snapshots.stream()
                .map(AuthorHeatSnapshotDTO::fromEntity)
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @GetMapping("/videos")
    public ApiResponse<List<VideoHeatSnapshotDTO>> getVideoHeatSnapshotsByTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime snapshotTime,
            @RequestParam(defaultValue = "50") int limit) {
        List<VideoHeatSnapshot> snapshots = heatSnapshotService.getVideoHeatSnapshotsByTime(snapshotTime, limit);
        List<VideoHeatSnapshotDTO> dtos = snapshots.stream()
                .map(VideoHeatSnapshotDTO::fromEntity)
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @GetMapping("/tags")
    public ApiResponse<List<TagHeatSnapshotDTO>> getTagHeatSnapshotsByTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime snapshotTime,
            @RequestParam(defaultValue = "50") int limit) {
        List<TagHeatSnapshot> snapshots = heatSnapshotService.getTagHeatSnapshotsByTime(snapshotTime, limit);
        List<TagHeatSnapshotDTO> dtos = snapshots.stream()
                .map(TagHeatSnapshotDTO::fromEntity)
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @GetMapping("/authors")
    public ApiResponse<List<AuthorHeatSnapshotDTO>> getAuthorHeatSnapshotsByTime(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime snapshotTime,
            @RequestParam(defaultValue = "50") int limit) {
        List<AuthorHeatSnapshot> snapshots = heatSnapshotService.getAuthorHeatSnapshotsByTime(snapshotTime, limit);
        List<AuthorHeatSnapshotDTO> dtos = snapshots.stream()
                .map(AuthorHeatSnapshotDTO::fromEntity)
                .collect(Collectors.toList());
        return ApiResponse.success(dtos);
    }

    @GetMapping("/latest-time")
    public ApiResponse<LocalDateTime> getLatestSnapshotTime() {
        LocalDateTime latestTime = heatSnapshotService.getLatestSnapshotTime();
        return ApiResponse.success(latestTime);
    }

    @GetMapping("/available-times")
    public ApiResponse<List<LocalDateTime>> getAvailableSnapshotTimes(
            @RequestParam(defaultValue = "24") int limit) {
        List<LocalDateTime> times = heatSnapshotService.getAvailableSnapshotTimes(limit);
        return ApiResponse.success(times);
    }

    @PostMapping("/generate")
    public ApiResponse<String> generateSnapshots(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime snapshotTime) {
        LocalDateTime time = snapshotTime != null ? snapshotTime : LocalDateTime.now();
        heatSnapshotService.generateAllSnapshots(time);
        return ApiResponse.success("快照生成成功", time.toString());
    }
}
