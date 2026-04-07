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

import com.klef.fsad.sdp.dto.TaskDto;
import com.klef.fsad.sdp.exception.ResourceNotFoundException;
import com.klef.fsad.sdp.model.Group;
import com.klef.fsad.sdp.model.Task;
import com.klef.fsad.sdp.model.User;
import com.klef.fsad.sdp.repository.GroupRepository;
import com.klef.fsad.sdp.repository.TaskRepository;
import com.klef.fsad.sdp.repository.UserRepository;
import com.klef.fsad.sdp.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<TaskDto> createTask(@RequestBody TaskDto taskDto) {
        Task savedTask = taskService.createTask(toEntity(taskDto));
        return ResponseEntity.ok(toDto(savedTask));
    }

    @GetMapping
    public ResponseEntity<List<TaskDto>> getAllTasks() {
        List<TaskDto> tasks = taskService.getAllTasks().stream().map(this::toDto).toList();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable Long id) {
        Optional<Task> task = taskRepository.findById(id);
        return task.map(value -> ResponseEntity.ok(toDto(value))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDto> updateTask(@PathVariable Long id, @RequestBody TaskDto updatedTask) {
        return taskRepository.findById(id).map(task -> {
            task.setTitle(updatedTask.getTitle());
            task.setDescription(updatedTask.getDescription());
            task.setStatus(updatedTask.getStatus());
            if (updatedTask.getGroupId() != null) {
                Group group = groupRepository.findById(updatedTask.getGroupId())
                    .orElseThrow(
                        () -> new ResourceNotFoundException("Group not found with id: " + updatedTask.getGroupId()));
                task.setGroup(group);
            }
            if (updatedTask.getAssignedToUserId() != null) {
                User assignedUser = userRepository.findById(updatedTask.getAssignedToUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                                "User not found with id: " + updatedTask.getAssignedToUserId()));
                task.setAssignedTo(assignedUser);
            }
            return ResponseEntity.ok(toDto(taskRepository.save(task)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        if (!taskRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        taskRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{taskId}/assign/{userId}")
    public ResponseEntity<TaskDto> assignTask(@PathVariable Long taskId, @PathVariable Long userId) {
        return ResponseEntity.ok(toDto(taskService.assignTask(taskId, userId)));
    }

    @PutMapping("/{taskId}/status/{status}")
    public ResponseEntity<TaskDto> updateTaskStatus(@PathVariable Long taskId, @PathVariable String status) {
        return ResponseEntity.ok(toDto(taskService.updateStatus(taskId, status)));
    }

    private TaskDto toDto(Task task) {
        return TaskDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .groupId(task.getGroup() != null ? task.getGroup().getId() : null)
                .assignedToUserId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .build();
    }

    private Task toEntity(TaskDto taskDto) {
        Group group = null;
        if (taskDto.getGroupId() != null) {
            group = groupRepository.findById(taskDto.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + taskDto.getGroupId()));
        }

        User assignedUser = null;
        if (taskDto.getAssignedToUserId() != null) {
            assignedUser = userRepository.findById(taskDto.getAssignedToUserId())
                .orElseThrow(
                    () -> new ResourceNotFoundException("User not found with id: " + taskDto.getAssignedToUserId()));
        }

        return Task.builder()
                .id(taskDto.getId())
                .title(taskDto.getTitle())
                .description(taskDto.getDescription())
                .status(taskDto.getStatus())
                .group(group)
                .assignedTo(assignedUser)
                .build();
    }
}
