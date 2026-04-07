package com.klef.fsad.sdp.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klef.fsad.sdp.exception.ResourceNotFoundException;
import com.klef.fsad.sdp.model.Group;
import com.klef.fsad.sdp.model.Project;
import com.klef.fsad.sdp.model.Submission;
import com.klef.fsad.sdp.model.User;
import com.klef.fsad.sdp.repository.GroupRepository;
import com.klef.fsad.sdp.repository.ProjectRepository;
import com.klef.fsad.sdp.repository.SubmissionRepository;
import com.klef.fsad.sdp.repository.UserRepository;
import com.klef.fsad.sdp.service.ProjectService;

@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Project createProject(Project project) {
        return projectRepository.save(project);
    }

    @Override
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    @Override
    public List<Project> getProjectsByGroup(Long groupId) {
        return projectRepository.findByGroupId(groupId);
    }

    @Override
    public List<Project> getProjectsForUser(Long userId, String role) {
        if (isAdminRole(role)) {
            return projectRepository.findAll();
        }

        if (userId == null) {
            return List.of();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Long groupId = user.getGroup() != null ? user.getGroup().getId() : null;
        if (groupId == null) {
            return List.of();
        }

        return projectRepository.findByGroupId(groupId);
    }

    private boolean isAdminRole(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        return "ADMIN".equals(normalized) || "TEACHER".equals(normalized);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        List<Group> groups = groupRepository.findByProjectId(projectId);
        for (Group group : groups) {
            group.setProject(null);
        }
        if (!groups.isEmpty()) {
            groupRepository.saveAll(groups);
        }

        List<Submission> submissions = submissionRepository.findByProjectId(projectId);
        for (Submission submission : submissions) {
            submission.setProject(null);
        }
        if (!submissions.isEmpty()) {
            submissionRepository.saveAll(submissions);
        }

        projectRepository.delete(project);
    }
}
