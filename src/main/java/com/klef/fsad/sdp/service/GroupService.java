package com.klef.fsad.sdp.service;

import java.util.List;

import com.klef.fsad.sdp.model.Group;

public interface GroupService {

    Group createGroup(Group group);

    List<Group> getAllGroups();

    Group assignTaskToGroup(Long groupId, Long taskId);

    Group assignUserToGroup(Long groupId, Long userId);

    void deleteGroup(Long groupId);
}
