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

import com.klef.fsad.sdp.dto.GroupDto;
import com.klef.fsad.sdp.exception.ResourceNotFoundException;
import com.klef.fsad.sdp.model.Group;
import com.klef.fsad.sdp.model.Project;
import com.klef.fsad.sdp.repository.GroupRepository;
import com.klef.fsad.sdp.repository.ProjectRepository;
import com.klef.fsad.sdp.service.GroupService;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @PostMapping
    public ResponseEntity<GroupDto> createGroup(@RequestBody GroupDto groupDto) {
        Group savedGroup = groupService.createGroup(toEntity(groupDto));
        return ResponseEntity.ok(toDto(savedGroup));
    }

    @GetMapping
    public ResponseEntity<List<GroupDto>> getAllGroups() {
        List<GroupDto> groups = groupService.getAllGroups().stream().map(this::toDto).toList();
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupDto> getGroupById(@PathVariable Long id) {
        Optional<Group> group = groupRepository.findById(id);
        return group.map(value -> ResponseEntity.ok(toDto(value))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupDto> updateGroup(@PathVariable Long id, @RequestBody GroupDto updatedGroupDto) {
        return groupRepository.findById(id).map(group -> {
            group.setGroupName(updatedGroupDto.getGroupName());
            if (updatedGroupDto.getProjectId() != null) {
                Project project = projectRepository.findById(updatedGroupDto.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + updatedGroupDto.getProjectId()));
                group.setProject(project);
            }
            return ResponseEntity.ok(toDto(groupRepository.save(group)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{groupId}/assign-user/{userId}")
    public ResponseEntity<GroupDto> assignUserToGroup(@PathVariable Long groupId, @PathVariable Long userId) {
        return ResponseEntity.ok(toDto(groupService.assignUserToGroup(groupId, userId)));
    }

    @PutMapping("/{groupId}/assign-task/{taskId}")
    public ResponseEntity<GroupDto> assignTaskToGroup(@PathVariable Long groupId, @PathVariable Long taskId) {
        return ResponseEntity.ok(toDto(groupService.assignTaskToGroup(groupId, taskId)));
    }

    private GroupDto toDto(Group group) {
        return GroupDto.builder()
                .id(group.getId())
                .groupName(group.getGroupName())
                .projectId(group.getProject() != null ? group.getProject().getId() : null)
                .build();
    }

    private Group toEntity(GroupDto groupDto) {
        Project project = null;
        if (groupDto.getProjectId() != null) {
            project = projectRepository.findById(groupDto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + groupDto.getProjectId()));
        }

        return Group.builder()
                .id(groupDto.getId())
                .groupName(groupDto.getGroupName())
                .project(project)
                .build();
    }
}
