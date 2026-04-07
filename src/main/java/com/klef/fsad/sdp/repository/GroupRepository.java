package com.klef.fsad.sdp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klef.fsad.sdp.model.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

	List<Group> findByProjectId(Long projectId);
}
