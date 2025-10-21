package org.example.musicalinstrumentsystem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.musicalinstrumentsystem.entity.FinancialReport;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.repository.FinancialReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FinancialReportService {

    @Autowired
    private FinancialReportRepository financialReportRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    //  CREATE OPERATION
    public FinancialReport saveReport(
            String reportName,
            String reportPeriod,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Double totalRevenue,
            Double pendingRevenue,
            Long totalTransactions,
            Double averageOrderValue,
            Double growthRate,
            Integer ordersInRange,
            Map<String, Double> salesByCategory,
            List<Map.Entry<String, Double>> topProducts,
            String notes,
            User createdBy) {

        try {
            System.out.println("=== SAVING FINANCIAL REPORT ===");
            System.out.println("Report Name: " + reportName);

            FinancialReport report = new FinancialReport();
            report.setReportName(reportName);
            report.setReportPeriod(reportPeriod);
            report.setStartDate(startDate);
            report.setEndDate(endDate);
            report.setTotalRevenue(totalRevenue);
            report.setPendingRevenue(pendingRevenue);
            report.setTotalTransactions(totalTransactions);
            report.setAverageOrderValue(averageOrderValue);
            report.setGrowthRate(growthRate);
            report.setOrdersInRange(ordersInRange);
            report.setNotes(notes);
            report.setCreatedBy(createdBy);

            // Convert maps to JSON strings
            if (salesByCategory != null) {
                report.setSalesByCategoryJson(objectMapper.writeValueAsString(salesByCategory));
            }
            if (topProducts != null) {
                report.setTopProductsJson(objectMapper.writeValueAsString(topProducts));
            }

            FinancialReport savedReport = financialReportRepository.save(report);
            System.out.println("✅ Report saved with ID: " + savedReport.getId());
            return savedReport;

        } catch (JsonProcessingException e) {
            System.out.println("❌ Error saving report: " + e.getMessage());
            throw new RuntimeException("Failed to save financial report", e);
        }
    }

    //  READ OPERATIONS

    // Get all saved reports
    public List<FinancialReport> getAllReports() {
        try {
            System.out.println("=== FETCHING ALL FINANCIAL REPORTS ===");
            List<FinancialReport> reports = financialReportRepository.findAllByOrderByCreatedAtDesc();
            System.out.println(" Found " + reports.size() + " reports");
            return reports;
        } catch (Exception e) {
            System.out.println(" Error fetching reports: " + e.getMessage());
            return List.of();
        }
    }

    // Get report by ID
    public Optional<FinancialReport> getReportById(Long id) {
        try {
            System.out.println("=== FETCHING REPORT BY ID: " + id + " ===");
            Optional<FinancialReport> report = financialReportRepository.findById(id);
            if (report.isPresent()) {
                System.out.println(" Report found: " + report.get().getReportName());
            } else {
                System.out.println(" Report not found");
            }
            return report;
        } catch (Exception e) {
            System.out.println(" Error fetching report: " + e.getMessage());
            return Optional.empty();
        }
    }

    // Get reports by user
    public List<FinancialReport> getReportsByUser(User user) {
        try {
            System.out.println("=== FETCHING REPORTS BY USER: " + user.getEmail() + " ===");
            List<FinancialReport> reports = financialReportRepository.findByCreatedByOrderByCreatedAtDesc(user);
            System.out.println(" Found " + reports.size() + " reports for user");
            return reports;
        } catch (Exception e) {
            System.out.println(" Error fetching user reports: " + e.getMessage());
            return List.of();
        }
    }

    // Get reports by period
    public List<FinancialReport> getReportsByPeriod(String period) {
        try {
            System.out.println("=== FETCHING REPORTS BY PERIOD: " + period + " ===");
            List<FinancialReport> reports = financialReportRepository.findByReportPeriodOrderByCreatedAtDesc(period);
            System.out.println(" Found " + reports.size() + " reports for period");
            return reports;
        } catch (Exception e) {
            System.out.println(" Error fetching period reports: " + e.getMessage());
            return List.of();
        }
    }

    // UPDATE OPERATION
    public FinancialReport updateReport(Long id, String reportName, String notes) {
        try {
            System.out.println("=== UPDATING REPORT ID: " + id + " ===");
            Optional<FinancialReport> reportOpt = financialReportRepository.findById(id);

            if (reportOpt.isPresent()) {
                FinancialReport report = reportOpt.get();
                if (reportName != null && !reportName.isEmpty()) {
                    report.setReportName(reportName);
                }
                if (notes != null) {
                    report.setNotes(notes);
                }
                FinancialReport updated = financialReportRepository.save(report);
                System.out.println(" Report updated successfully");
                return updated;
            } else {
                System.out.println(" Report not found");
                return null;
            }
        } catch (Exception e) {
            System.out.println(" Error updating report: " + e.getMessage());
            throw new RuntimeException("Failed to update report", e);
        }
    }

    // DELETE OPERATION
    public boolean deleteReport(Long id) {
        try {
            System.out.println("=== DELETING REPORT ID: " + id + " ===");
            if (financialReportRepository.existsById(id)) {
                financialReportRepository.deleteById(id);
                System.out.println(" Report deleted successfully");
                return true;
            } else {
                System.out.println(" Report not found");
                return false;
            }
        } catch (Exception e) {
            System.out.println(" Error deleting report: " + e.getMessage());
            return false;
        }
    }

    // Get total saved reports count
    public long getTotalReportsCount() {
        return financialReportRepository.count();
}
}
