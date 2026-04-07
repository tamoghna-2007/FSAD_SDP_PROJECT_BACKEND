package com.klef.fsad.sdp.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klef.fsad.sdp.exception.ResourceNotFoundException;
import com.klef.fsad.sdp.model.Group;
import com.klef.fsad.sdp.model.Task;
import com.klef.fsad.sdp.model.User;
import com.klef.fsad.sdp.repository.GroupRepository;
import com.klef.fsad.sdp.repository.TaskRepository;
import com.klef.fsad.sdp.repository.UserRepository;
import com.klef.fsad.sdp.service.GroupService;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Group createGroup(Group group) {
        return groupRepository.save(group);
    }

    @Override
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    @Override
    public Group assignTaskToGroup(Long groupId, Long taskId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        task.setGroup(group);
        taskRepository.save(task);
        return group;
    }

    @Override
    public Group assignUserToGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setGroup(group);
        userRepository.save(user);
        return group;
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));

        List<User> users = userRepository.findByGroupId(groupId);
        for (User user : users) {
            user.setGroup(null);
        }
        if (!users.isEmpty()) {
            userRepository.saveAll(users);
        }

        List<Task> tasks = taskRepository.findByGroupId(groupId);
        for (Task task : tasks) {
            task.setGroup(null);
        }
        if (!tasks.isEmpty()) {
            taskRepository.saveAll(tasks);
        }

        groupRepository.delete(group);
    }
}
