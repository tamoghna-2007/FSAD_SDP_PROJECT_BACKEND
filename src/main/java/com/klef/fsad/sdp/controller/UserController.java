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

import com.klef.fsad.sdp.dto.UserDto;
import com.klef.fsad.sdp.exception.ResourceNotFoundException;
import com.klef.fsad.sdp.model.Group;
import com.klef.fsad.sdp.model.User;
import com.klef.fsad.sdp.repository.GroupRepository;
import com.klef.fsad.sdp.repository.UserRepository;
import com.klef.fsad.sdp.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        User savedUser = userService.createUser(toEntity(userDto));
        return ResponseEntity.ok(toDto(savedUser));
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers().stream().map(this::toDto).toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(value -> ResponseEntity.ok(toDto(value))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UserDto updatedUser) {
        return userRepository.findById(id).map(user -> {
            user.setName(updatedUser.getName());
            user.setEmail(updatedUser.getEmail());
            user.setRole(updatedUser.getRole());
            if (updatedUser.getGroupId() != null) {
                Group group = groupRepository.findById(updatedUser.getGroupId())
                    .orElseThrow(
                        () -> new ResourceNotFoundException("Group not found with id: " + updatedUser.getGroupId()));
                user.setGroup(group);
            }
            return ResponseEntity.ok(toDto(userRepository.save(user)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/assign-group/{groupId}")
    public ResponseEntity<UserDto> assignUserToGroup(@PathVariable Long userId, @PathVariable Long groupId) {
        return ResponseEntity.ok(toDto(userService.assignUserToGroup(userId, groupId)));
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .groupId(user.getGroup() != null ? user.getGroup().getId() : null)
                .build();
    }

    private User toEntity(UserDto userDto) {
        Group group = null;
        if (userDto.getGroupId() != null) {
            group = groupRepository.findById(userDto.getGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + userDto.getGroupId()));
        }

        return User.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .email(userDto.getEmail())
                .role(userDto.getRole())
                .group(group)
                .build();
    }
}
