package com.klef.fsad.sdp.service;

import java.util.List;

import com.klef.fsad.sdp.model.Submission;

public interface SubmissionService {

    Submission createSubmission(Submission submission);

    List<Submission> getAllSubmissions();

    List<Submission> getSubmissionsByUser(Long userId);

    Submission submitProject(Long projectId, String fileUrl);
}
