package com.klef.fsad.sdp.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionDto {

    private Long id;
    private String fileUrl;
    private LocalDateTime timestamp;
    private Double grade;
    private Long projectId;
    private Long userId;
}
