package com.bookdhara.safety;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, Report.TargetType type, Long targetId);

    long countByReporterIdAndCreatedAtAfter(Long reporterId, java.time.Instant after);
}
