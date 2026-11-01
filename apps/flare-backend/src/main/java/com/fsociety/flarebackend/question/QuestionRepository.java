package com.fsociety.flarebackend.question;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

	/** True when the Report was made by "Send to staff anyway": the classifier must then return REPORT. */
	boolean existsBySentToStaffReportId(UUID reportId);
}
