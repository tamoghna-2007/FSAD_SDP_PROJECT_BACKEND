package com.klef.fsad.sdp.service;

import java.util.List;

import com.klef.fsad.sdp.model.User;

public interface UserService {

    User createUser(User user);

    List<User> getAllUsers();

    User assignUserToGroup(Long userId, Long groupId);
}
