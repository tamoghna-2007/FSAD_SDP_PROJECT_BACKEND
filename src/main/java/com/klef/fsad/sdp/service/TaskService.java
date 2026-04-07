package com.klef.fsad.sdp.service;

import java.util.List;

import com.klef.fsad.sdp.model.Task;

public interface TaskService {

    Task createTask(Task task);

    List<Task> getAllTasks();

    Task assignTask(Long taskId, Long userId);

    Task updateStatus(Long taskId, String status);
}
