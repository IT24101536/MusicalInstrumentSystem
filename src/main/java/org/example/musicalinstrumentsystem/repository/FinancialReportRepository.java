package org.example.musicalinstrumentsystem.repository;

import org.example.musicalinstrumentsystem.entity.FinancialReport;
import org.example.musicalinstrumentsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FinancialReportRepository extends JpaRepository<FinancialReport, Long> {

    // Find reports by created user
    List<FinancialReport> findByCreatedByOrderByCreatedAtDesc(User createdBy);

    // Find reports by period
    List<FinancialReport> findByReportPeriodOrderByCreatedAtDesc(String reportPeriod);

    // Find reports within a date range
    List<FinancialReport> findByStartDateBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    // Find all reports ordered by creation date
    List<FinancialReport> findAllByOrderByCreatedAtDesc();
}
