package com.klef.fsad.sdp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.klef.fsad.sdp.model.Submission;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

	List<Submission> findByProjectId(Long projectId);

	List<Submission> findByUserId(Long userId);
}
