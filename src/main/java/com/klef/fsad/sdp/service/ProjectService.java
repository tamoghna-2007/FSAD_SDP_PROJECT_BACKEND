package com.klef.fsad.sdp.service;

import java.util.List;

import com.klef.fsad.sdp.model.Project;

public interface ProjectService {

    Project createProject(Project project);

    List<Project> getAllProjects();

    List<Project> getProjectsByGroup(Long groupId);

    List<Project> getProjectsForUser(Long userId, String role);

    void deleteProject(Long projectId);
}
