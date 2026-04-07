package com.klef.fsad.sdp.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klef.fsad.sdp.dto.ProjectDto;
import com.klef.fsad.sdp.exception.ResourceNotFoundException;
import com.klef.fsad.sdp.model.Group;
import com.klef.fsad.sdp.model.Project;
import com.klef.fsad.sdp.model.User;
import com.klef.fsad.sdp.repository.GroupRepository;
import com.klef.fsad.sdp.repository.ProjectRepository;
import com.klef.fsad.sdp.repository.UserRepository;
import com.klef.fsad.sdp.service.ProjectService;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ProjectDto> createProject(@RequestBody ProjectDto projectDto) {
        Project savedProject = projectService.createProject(toEntity(projectDto));
        return ResponseEntity.ok(toDto(savedProject));
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> getAllProjects(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role) {
        List<Project> projects;

        if (role == null || role.isBlank()) {
            projects = projectService.getAllProjects();
        } else {
            projects = projectService.getProjectsForUser(userId, role);
        }

        List<ProjectDto> response = projects.stream().map(this::toDto).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<ProjectDto>> getProjectsByGroup(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role) {
        if (!canAccessGroup(role, userId, groupId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<ProjectDto> projects = projectService.getProjectsByGroup(groupId).stream().map(this::toDto).toList();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProjectById(@PathVariable Long id) {
        Optional<Project> project = projectRepository.findById(id);
        return project.map(value -> ResponseEntity.ok(toDto(value))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> updateProject(@PathVariable Long id, @RequestBody ProjectDto updatedProject) {
        return projectRepository.findById(id).map(project -> {
            project.setTitle(updatedProject.getTitle());
            project.setDescription(updatedProject.getDescription());
            project.setDeadline(updatedProject.getDeadline());
            if (updatedProject.getGroupId() != null) {
                Group group = groupRepository.findById(updatedProject.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + updatedProject.getGroupId()));
                project.setGroup(group);
            }
            return ResponseEntity.ok(toDto(projectRepository.save(project)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    private ProjectDto toDto(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .deadline(project.getDeadline())
                .groupId(project.getGroup() != null ? project.getGroup().getId() : null)
                .build();
    }

    private Project toEntity(ProjectDto projectDto) {
        Group group = null;
        if (projectDto.getGroupId() != null) {
            group = groupRepository.findById(projectDto.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + projectDto.getGroupId()));
        }

        return Project.builder()
                .id(projectDto.getId())
                .title(projectDto.getTitle())
                .description(projectDto.getDescription())
                .deadline(projectDto.getDeadline())
                .group(group)
                .build();
    }

    private boolean canAccessGroup(String role, Long userId, Long groupId) {
        if (isAdminRole(role) || role == null || role.isBlank()) {
            return true;
        }

        if (userId == null) {
            return false;
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Long userGroupId = user.getGroup() != null ? user.getGroup().getId() : null;
        return userGroupId != null && userGroupId.equals(groupId);
    }

    private boolean isAdminRole(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toUpperCase();
        return "ADMIN".equals(normalized) || "TEACHER".equals(normalized);
    }
}
