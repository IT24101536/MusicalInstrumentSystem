package org.example.musicalinstrumentsystem.repository;

import org.example.musicalinstrumentsystem.entity.ReportSchedule;
import org.example.musicalinstrumentsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {

    List<ReportSchedule> findByIsActiveTrueOrderByNextExecutionAsc();

    List<ReportSchedule> findByCreatedByOrderByCreatedAtDesc(User createdBy);

    List<ReportSchedule> findByFrequencyOrderByCreatedAtDesc(String frequency);

    List<ReportSchedule> findByReportTypeOrderByCreatedAtDesc(String reportType);

    List<ReportSchedule> findByIsActiveTrueAndNextExecutionBefore(LocalDateTime dateTime);

    long countByIsActiveTrue();
}

