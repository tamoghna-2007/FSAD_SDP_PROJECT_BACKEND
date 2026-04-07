package com.klef.fsad.sdp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klef.fsad.sdp.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	List<Project> findByGroupId(Long groupId);
}
