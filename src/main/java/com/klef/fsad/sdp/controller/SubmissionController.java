package com.klef.fsad.sdp.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klef.fsad.sdp.dto.SubmissionDto;
import com.klef.fsad.sdp.exception.ResourceNotFoundException;
import com.klef.fsad.sdp.model.Project;
import com.klef.fsad.sdp.model.Submission;
import com.klef.fsad.sdp.model.User;
import com.klef.fsad.sdp.repository.ProjectRepository;
import com.klef.fsad.sdp.repository.SubmissionRepository;
import com.klef.fsad.sdp.repository.UserRepository;
import com.klef.fsad.sdp.service.SubmissionService;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<SubmissionDto> createSubmission(@RequestBody SubmissionDto submissionDto) {
        Submission savedSubmission = submissionService.createSubmission(toEntity(submissionDto));
        return ResponseEntity.ok(toDto(savedSubmission));
    }

    @GetMapping
    public ResponseEntity<List<SubmissionDto>> getAllSubmissions() {
        List<SubmissionDto> submissions = submissionService.getAllSubmissions().stream().map(this::toDto).toList();
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubmissionDto>> getSubmissionsByUser(@PathVariable Long userId) {
        List<SubmissionDto> submissions = submissionService.getSubmissionsByUser(userId).stream().map(this::toDto).toList();
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDto> getSubmissionById(@PathVariable Long id) {
        Optional<Submission> submission = submissionRepository.findById(id);
        return submission.map(value -> ResponseEntity.ok(toDto(value))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubmissionDto> updateSubmission(@PathVariable Long id, @RequestBody SubmissionDto updatedSubmissionDto) {
        return submissionRepository.findById(id).map(submission -> {
            submission.setFileUrl(updatedSubmissionDto.getFileUrl());
            submission.setTimestamp(updatedSubmissionDto.getTimestamp());
            submission.setGrade(updatedSubmissionDto.getGrade());
            if (updatedSubmissionDto.getProjectId() != null) {
                Project project = projectRepository.findById(updatedSubmissionDto.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + updatedSubmissionDto.getProjectId()));
                submission.setProject(project);
            }
            if (updatedSubmissionDto.getUserId() != null) {
                User user = userRepository.findById(updatedSubmissionDto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + updatedSubmissionDto.getUserId()));
                submission.setUser(user);
            }
            return ResponseEntity.ok(toDto(submissionRepository.save(submission)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long id) {
        if (!submissionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        submissionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/submit/project/{projectId}")
    public ResponseEntity<SubmissionDto> submitProject(@PathVariable Long projectId, @RequestBody SubmitProjectRequest request) {
        return ResponseEntity.ok(toDto(submissionService.submitProject(projectId, request.fileUrl())));
    }

    private SubmissionDto toDto(Submission submission) {
        return SubmissionDto.builder()
                .id(submission.getId())
                .fileUrl(submission.getFileUrl())
                .timestamp(submission.getTimestamp())
            .grade(submission.getGrade())
                .projectId(submission.getProject() != null ? submission.getProject().getId() : null)
                .userId(submission.getUser() != null ? submission.getUser().getId() : null)
                .build();
    }

    private Submission toEntity(SubmissionDto submissionDto) {
        Project project = null;
        User user = null;
        if (submissionDto.getProjectId() != null) {
            project = projectRepository.findById(submissionDto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + submissionDto.getProjectId()));
        }

        if (submissionDto.getUserId() != null) {
            user = userRepository.findById(submissionDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + submissionDto.getUserId()));
        }

        return Submission.builder()
                .id(submissionDto.getId())
                .fileUrl(submissionDto.getFileUrl())
                .timestamp(submissionDto.getTimestamp())
            .grade(submissionDto.getGrade())
                .project(project)
                .user(user)
                .build();
    }

    public record SubmitProjectRequest(String fileUrl) {
    }
}
