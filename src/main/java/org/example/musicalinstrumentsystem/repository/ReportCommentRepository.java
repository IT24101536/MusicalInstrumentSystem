package org.example.musicalinstrumentsystem.repository;

import org.example.musicalinstrumentsystem.entity.FinancialReport;
import org.example.musicalinstrumentsystem.entity.ReportComment;
import org.example.musicalinstrumentsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {

    List<ReportComment> findByFinancialReportOrderByCreatedAtDesc(FinancialReport financialReport);

    List<ReportComment> findByUserOrderByCreatedAtDesc(User user);

    List<ReportComment> findByCommentTypeOrderByCreatedAtDesc(String commentType);

    List<ReportComment> findByFinancialReportAndCommentTypeOrderByCreatedAtDesc(
            FinancialReport financialReport, String commentType);

    long countByFinancialReport(FinancialReport financialReport);
}

