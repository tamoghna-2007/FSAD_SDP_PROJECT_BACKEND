package com.klef.fsad.sdp.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.klef.fsad.sdp.exception.ResourceNotFoundException;
import com.klef.fsad.sdp.model.Project;
import com.klef.fsad.sdp.model.Submission;
import com.klef.fsad.sdp.repository.ProjectRepository;
import com.klef.fsad.sdp.repository.SubmissionRepository;
import com.klef.fsad.sdp.service.SubmissionService;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public Submission createSubmission(Submission submission) {
        if (submission.getTimestamp() == null) {
            submission.setTimestamp(LocalDateTime.now());
        }
        return submissionRepository.save(submission);
    }

    @Override
    public List<Submission> getAllSubmissions() {
        return submissionRepository.findAll();
    }

    @Override
    public List<Submission> getSubmissionsByUser(Long userId) {
        return submissionRepository.findByUserId(userId);
    }

    @Override
    public Submission submitProject(Long projectId, String fileUrl) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        Submission submission = Submission.builder()
                .fileUrl(fileUrl)
                .timestamp(LocalDateTime.now())
                .project(project)
                .build();

        return submissionRepository.save(submission);
    }
}
