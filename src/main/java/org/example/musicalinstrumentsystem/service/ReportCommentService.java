package org.example.musicalinstrumentsystem.service;

import org.example.musicalinstrumentsystem.entity.FinancialReport;
import org.example.musicalinstrumentsystem.entity.ReportComment;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.repository.ReportCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReportCommentService {

    @Autowired
    private ReportCommentRepository reportCommentRepository;

    public ReportComment createComment(FinancialReport report, User user, String commentText, String commentType) {
        try {
            System.out.println("=== CREATING REPORT COMMENT ===");
            System.out.println("Report ID: " + report.getId());
            System.out.println("User: " + user.getEmail());
            System.out.println("Type: " + commentType);

            ReportComment comment = new ReportComment();
            comment.setFinancialReport(report);
            comment.setUser(user);
            comment.setCommentText(commentText);
            comment.setCommentType(commentType);
            comment.setCreatedAt(LocalDateTime.now());

            ReportComment savedComment = reportCommentRepository.save(comment);
            System.out.println("✅ Comment created with ID: " + savedComment.getId());
            return savedComment;

        } catch (Exception e) {
            System.out.println("❌ Error creating comment: " + e.getMessage());
            throw new RuntimeException("Failed to create comment", e);
        }
    }

    public List<ReportComment> getCommentsByReport(FinancialReport report) {
        try {
            System.out.println("=== FETCHING COMMENTS FOR REPORT: " + report.getId() + " ===");
            List<ReportComment> comments = reportCommentRepository.findByFinancialReportOrderByCreatedAtDesc(report);
            System.out.println("✅ Found " + comments.size() + " comments");
            return comments;
        } catch (Exception e) {
            System.out.println("❌ Error fetching comments: " + e.getMessage());
            return List.of();
        }
    }

    public Optional<ReportComment> getCommentById(Long id) {
        try {
            System.out.println("=== FETCHING COMMENT BY ID: " + id + " ===");
            return reportCommentRepository.findById(id);
        } catch (Exception e) {
            System.out.println("❌ Error fetching comment: " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<ReportComment> getCommentsByUser(User user) {
        try {
            System.out.println("=== FETCHING COMMENTS BY USER: " + user.getEmail() + " ===");
            return reportCommentRepository.findByUserOrderByCreatedAtDesc(user);
        } catch (Exception e) {
            System.out.println("❌ Error fetching user comments: " + e.getMessage());
            return List.of();
        }
    }

    public List<ReportComment> getCommentsByType(String commentType) {
        try {
            System.out.println("=== FETCHING COMMENTS BY TYPE: " + commentType + " ===");
            return reportCommentRepository.findByCommentTypeOrderByCreatedAtDesc(commentType);
        } catch (Exception e) {
            System.out.println("❌ Error fetching comments by type: " + e.getMessage());
            return List.of();
        }
    }

    public long countCommentsByReport(FinancialReport report) {
        return reportCommentRepository.countByFinancialReport(report);
    }

    public ReportComment updateComment(Long id, String commentText) {
        try {
            System.out.println("=== UPDATING COMMENT ID: " + id + " ===");
            Optional<ReportComment> commentOpt = reportCommentRepository.findById(id);

            if (commentOpt.isPresent()) {
                ReportComment comment = commentOpt.get();
                comment.setCommentText(commentText);
                comment.setUpdatedAt(LocalDateTime.now());

                ReportComment updated = reportCommentRepository.save(comment);
                System.out.println("✅ Comment updated successfully");
                return updated;
            } else {
                System.out.println("❌ Comment not found");
                return null;
            }
        } catch (Exception e) {
            System.out.println("❌ Error updating comment: " + e.getMessage());
            throw new RuntimeException("Failed to update comment", e);
        }
    }

    public boolean deleteComment(Long id) {
        try {
            System.out.println("=== DELETING COMMENT ID: " + id + " ===");
            if (reportCommentRepository.existsById(id)) {
                reportCommentRepository.deleteById(id);
                System.out.println("✅ Comment deleted successfully");
                return true;
            } else {
                System.out.println("❌ Comment not found");
                return false;
            }
        } catch (Exception e) {
            System.out.println("❌ Error deleting comment: " + e.getMessage());
            return false;
        }
    }
}

