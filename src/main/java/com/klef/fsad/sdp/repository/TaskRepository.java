package com.klef.fsad.sdp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klef.fsad.sdp.model.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {

	List<Task> findByGroupId(Long groupId);
}
