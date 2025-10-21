package org.example.musicalinstrumentsystem.service;

import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.Payment;
import org.example.musicalinstrumentsystem.payment.PaymentDetails;
import org.example.musicalinstrumentsystem.payment.PaymentResult;
import org.example.musicalinstrumentsystem.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(rollbackFor = Exception.class)
public class PaymentRecordService {

    @Autowired
    private PaymentRepository paymentRepository;


    public Payment savePayment(Payment payment) {
        try {
            System.out.println("=== SAVING PAYMENT RECORD ===");
            System.out.println("Order ID: " + (payment.getOrder() != null ? payment.getOrder().getId() : "NULL"));
            System.out.println("Amount: " + payment.getAmount());
            System.out.println("Currency: " + payment.getCurrency());
            System.out.println("Status: " + payment.getStatus());
            System.out.println("Transaction ID: " + payment.getTransactionId());
            System.out.println("Payment Method: " + payment.getPaymentMethod());
            System.out.println("Payment Date: " + payment.getPaymentDate());
            System.out.println("Created At: " + payment.getCreatedAt());
            System.out.println("Updated At: " + payment.getUpdatedAt());
            
            // Validate all required fields
            if (payment.getOrder() == null) {
                System.out.println("⚠ Warning: Order is null - this might be a test payment");
                // For test payments, we'll allow null order
            }
            if (payment.getTransactionId() == null || payment.getTransactionId().trim().isEmpty()) {
                throw new IllegalArgumentException("Transaction ID cannot be null or empty");
            }
            if (payment.getPaymentMethod() == null || payment.getPaymentMethod().trim().isEmpty()) {
                throw new IllegalArgumentException("Payment method cannot be null or empty");
            }
            if (payment.getAmount() == null || payment.getAmount() <= 0) {
                throw new IllegalArgumentException("Amount must be positive");
            }
            if (payment.getCurrency() == null || payment.getCurrency().trim().isEmpty()) {
                throw new IllegalArgumentException("Currency cannot be null or empty");
            }
            if (payment.getStatus() == null || payment.getStatus().trim().isEmpty()) {
                throw new IllegalArgumentException("Status cannot be null or empty");
            }
            if (payment.getPaymentDate() == null) {
                throw new IllegalArgumentException("Payment date cannot be null");
            }
            if (payment.getCreatedAt() == null) {
                payment.setCreatedAt(LocalDateTime.now());
            }
            if (payment.getUpdatedAt() == null) {
                payment.setUpdatedAt(LocalDateTime.now());
            }
            
            System.out.println(" All validation checks passed");
            
            Payment savedPayment = paymentRepository.save(payment);
            System.out.println(" Payment record saved with ID: " + savedPayment.getId());
            return savedPayment;
        } catch (Exception e) {
            System.out.println(" Error saving payment record: " + e.getMessage());
            System.out.println(" Error type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            throw new RuntimeException("Failed to save payment record: " + e.getMessage(), e);
        }
    }


    public Payment createPayment(Order order, PaymentDetails paymentDetails) {
        System.out.println("=== CREATING PAYMENT RECORD ===");
        System.out.println("Order ID: " + order.getId());
        System.out.println("Payment Method: " + paymentDetails.getPaymentMethod());

        try {
            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setPaymentMethod(paymentDetails.getPaymentMethod().getDisplayName());
            payment.setAmount(order.getTotalAmount());
            payment.setStatus("PENDING");
            payment.setTransactionId("TEMP_" + System.currentTimeMillis());
            payment.setPaymentDate(LocalDateTime.now());

            // Store payment details as JSON
            if (paymentDetails.getPaymentData() != null) {
                payment.setPaymentDetails(paymentDetails.getPaymentData().toString());
            }

            System.out.println("Payment record before save:");
            System.out.println("  - Order: " + payment.getOrder().getId());
            System.out.println("  - Method: " + payment.getPaymentMethod());
            System.out.println("  - Amount: " + payment.getAmount());
            System.out.println("  - Status: " + payment.getStatus());
            System.out.println("  - Transaction ID: " + payment.getTransactionId());
            System.out.println("  - Payment Date: " + payment.getPaymentDate());

            Payment savedPayment = paymentRepository.save(payment);
            System.out.println(" Payment record created with ID: " + savedPayment.getId());

            return savedPayment;
        } catch (Exception e) {
            System.out.println(" Error creating payment record: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create payment record", e);
        }
    }

    public Payment updatePaymentStatus(Long paymentId, PaymentResult paymentResult) {
        System.out.println("=== UPDATING PAYMENT STATUS ===");
        System.out.println("Payment ID: " + paymentId);
        System.out.println("Success: " + paymentResult.isSuccess());

        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();

                if (paymentResult.isSuccess()) {
                    payment.setStatus("COMPLETED");
                    if (paymentResult.getTransactionId() != null && !paymentResult.getTransactionId().isEmpty()) {
                        payment.setTransactionId(paymentResult.getTransactionId());
                    }
                    payment.setPaymentDate(LocalDateTime.now());
                    System.out.println(" Payment marked as COMPLETED");
                } else {
                    payment.setStatus("FAILED");
                    System.out.println(" Payment marked as FAILED");
                }

                payment.setUpdatedAt(LocalDateTime.now());

                System.out.println("Payment record before update:");
                System.out.println("  - Status: " + payment.getStatus());
                System.out.println("  - Transaction ID: " + payment.getTransactionId());
                System.out.println("  - Payment Date: " + payment.getPaymentDate());

                Payment updatedPayment = paymentRepository.save(payment);
                System.out.println(" Payment status updated successfully");

                return updatedPayment;
            }

            System.out.println(" Payment not found with ID: " + paymentId);
            return null;
        } catch (Exception e) {
            System.out.println(" Error updating payment status: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update payment status", e);
        }
    }

    public Payment completePayment(Order order, String transactionId, String paymentMethod) {
        System.out.println("=== COMPLETING PAYMENT RECORD ===");
        System.out.println("Order ID: " + order.getId());
        System.out.println("Transaction ID: " + transactionId);

        Optional<Payment> paymentOpt = paymentRepository.findByOrder(order);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus("COMPLETED");
            payment.setTransactionId(transactionId);
            payment.setPaymentMethod(paymentMethod);
            payment.setPaymentDate(LocalDateTime.now());

            Payment savedPayment = paymentRepository.save(payment);
            System.out.println(" Payment record completed successfully");

            return savedPayment;
        } else {
            // Create a new payment record if none exists
            System.out.println("⚠ No existing payment record found, creating new one");
            Payment payment = new Payment(order, transactionId, paymentMethod, order.getTotalAmount());
            payment.setStatus("COMPLETED");

            Payment savedPayment = paymentRepository.save(payment);
            System.out.println(" New payment record created and completed");

            return savedPayment;
        }
    }


    public Optional<Payment> getPaymentByOrder(Order order) {
        return paymentRepository.findByOrder(order);
    }


    public Optional<Payment> getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }


    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }


    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status);
    }


    public List<Payment> getRecentPayments() {
        return paymentRepository.findRecentPayments();
    }


    public Payment processRefund(Long paymentId, String reason) {
        System.out.println("=== PROCESSING PAYMENT REFUND ===");
        System.out.println("Payment ID: " + paymentId);
        System.out.println("Reason: " + reason);

        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();

            if (payment.canBeRefunded()) {
                payment.setStatus("REFUNDED");
                payment.setRefundDate(LocalDateTime.now());
                payment.setRefundReason(reason);

                Payment refundedPayment = paymentRepository.save(payment);
                System.out.println(" Payment refunded successfully");

                return refundedPayment;
            } else {
                System.out.println(" Payment cannot be refunded. Current status: " + payment.getStatus());
            }
        }

        return null;
    }

    // DELETION METHODS


    public boolean deletePayment(Long paymentId) {
        System.out.println("=== DELETING PAYMENT ===");
        System.out.println("Payment ID: " + paymentId);

        try {
            if (paymentRepository.existsById(paymentId)) {
                paymentRepository.deleteById(paymentId);
                System.out.println(" Payment deleted successfully: " + paymentId);
                return true;
            } else {
                System.out.println(" Payment not found with ID: " + paymentId);
                return false;
            }
        } catch (Exception e) {
            System.out.println(" Error deleting payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public boolean deletePaymentByOrder(Order order) {
        System.out.println("=== DELETING PAYMENT BY ORDER ===");
        System.out.println("Order ID: " + order.getId());

        try {
            Optional<Payment> paymentOpt = paymentRepository.findByOrder(order);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                paymentRepository.delete(payment);
                System.out.println(" Payment deleted for order: " + order.getId());
                return true;
            } else {
                System.out.println(" No payment found for order: " + order.getId());
                return false;
            }
        } catch (Exception e) {
            System.out.println(" Error deleting payment by order: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public boolean deletePaymentByTransactionId(String transactionId) {
        System.out.println("=== DELETING PAYMENT BY TRANSACTION ID ===");
        System.out.println("Transaction ID: " + transactionId);

        try {
            Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(transactionId);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                paymentRepository.delete(payment);
                System.out.println(" Payment deleted for transaction: " + transactionId);
                return true;
            } else {
                System.out.println(" No payment found for transaction: " + transactionId);
                return false;
            }
        } catch (Exception e) {
            System.out.println(" Error deleting payment by transaction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public boolean deletePayments(List<Long> paymentIds) {
        System.out.println("=== DELETING MULTIPLE PAYMENTS ===");
        System.out.println("Payment IDs: " + paymentIds);

        try {
            List<Payment> payments = paymentRepository.findAllById(paymentIds);
            if (!payments.isEmpty()) {
                paymentRepository.deleteAll(payments);
                System.out.println(" Deleted " + payments.size() + " payments");
                return true;
            } else {
                System.out.println(" No payments found with the provided IDs");
                return false;
            }
        } catch (Exception e) {
            System.out.println(" Error deleting multiple payments: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public boolean deleteAllPayments() {
        System.out.println("=== DELETING ALL PAYMENTS ===");

        try {
            long count = paymentRepository.count();
            if (count > 0) {
                paymentRepository.deleteAll();
                System.out.println(" Deleted all " + count + " payments");
                return true;
            } else {
                System.out.println("ℹ No payments to delete");
                return true;
            }
        } catch (Exception e) {
            System.out.println(" Error deleting all payments: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deletePaymentsByStatus(String status) {
        System.out.println("=== DELETING PAYMENTS BY STATUS ===");
        System.out.println("Status: " + status);

        try {
            List<Payment> payments = paymentRepository.findByStatus(status);
            if (!payments.isEmpty()) {
                paymentRepository.deleteAll(payments);
                System.out.println(" Deleted " + payments.size() + " payments with status: " + status);
                return true;
            } else {
                System.out.println(" No payments found with status: " + status);
                return true;
            }
        } catch (Exception e) {
            System.out.println(" Error deleting payments by status: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deletePaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        System.out.println("=== DELETING PAYMENTS BY DATE RANGE ===");
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);

        try {
            List<Payment> payments = paymentRepository.findByPaymentDateBetween(startDate, endDate);
            if (!payments.isEmpty()) {
                paymentRepository.deleteAll(payments);
                System.out.println(" Deleted " + payments.size() + " payments in date range");
                return true;
            } else {
                System.out.println("ℹ No payments found in the specified date range");
                return true;
            }
        } catch (Exception e) {
            System.out.println(" Error deleting payments by date range: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    //  STATISTICS METHODS

    public PaymentStatistics getPaymentStatistics() {
        System.out.println("=== CALCULATING PAYMENT STATISTICS ===");

        Double totalRevenue = paymentRepository.getTotalRevenue();
        Long totalPayments = paymentRepository.count();
        Long completedPayments = paymentRepository.countByStatus("COMPLETED");
        Long failedPayments = paymentRepository.countByStatus("FAILED");
        Long pendingPayments = paymentRepository.countByStatus("PENDING");
        Long refundedPayments = paymentRepository.countByStatus("REFUNDED");

        totalRevenue = totalRevenue != null ? totalRevenue : 0.0;

        System.out.println(" Payment Statistics:");
        System.out.println("   - Total Revenue: $" + totalRevenue);
        System.out.println("   - Total Payments: " + totalPayments);
        System.out.println("   - Completed: " + completedPayments);
        System.out.println("   - Failed: " + failedPayments);
        System.out.println("   - Pending: " + pendingPayments);
        System.out.println("   - Refunded: " + refundedPayments);

        return new PaymentStatistics(totalRevenue, totalPayments, completedPayments,
                failedPayments, pendingPayments, refundedPayments);
    }


    public long getTotalPaymentCount() {
        return paymentRepository.count();
    }


    public boolean paymentExists(Long paymentId) {
        return paymentRepository.existsById(paymentId);
    }

    // ========== ADVANCED SEARCH AND FILTERING ==========


    public List<Payment> searchPayments(String query, String status, String paymentMethod,
                                        String startDate, String endDate, Double minAmount, Double maxAmount) {
        System.out.println("=== SEARCHING PAYMENTS ===");
        System.out.println("Query: " + query);
        System.out.println("Status: " + status);
        System.out.println("Payment Method: " + paymentMethod);
        System.out.println("Date Range: " + startDate + " to " + endDate);
        System.out.println("Amount Range: " + minAmount + " to " + maxAmount);

        // For now, return all payments - in a real application, you would implement complex search logic
        List<Payment> allPayments = paymentRepository.findAll();
        
        // Apply basic filters
        return allPayments.stream()
                .filter(payment -> status == null || payment.getStatus().equals(status))
                .filter(payment -> paymentMethod == null || payment.getPaymentMethod().equals(paymentMethod))
                .filter(payment -> minAmount == null || payment.getAmount() >= minAmount)
                .filter(payment -> maxAmount == null || payment.getAmount() <= maxAmount)
                .toList();
    }


    public List<Payment> filterPayments(String status, String paymentMethod, String dateRange) {
        System.out.println("=== FILTERING PAYMENTS ===");
        System.out.println("Status: " + status);
        System.out.println("Payment Method: " + paymentMethod);
        System.out.println("Date Range: " + dateRange);

        List<Payment> allPayments = paymentRepository.findAll();
        
        return allPayments.stream()
                .filter(payment -> status == null || payment.getStatus().equals(status))
                .filter(payment -> paymentMethod == null || payment.getPaymentMethod().equals(paymentMethod))
                .toList();
    }

    //  BULK OPERATIONS


    public boolean bulkUpdateStatus(List<Long> paymentIds, String newStatus) {
        System.out.println("=== BULK UPDATING PAYMENT STATUS ===");
        System.out.println("Payment IDs: " + paymentIds);
        System.out.println("New Status: " + newStatus);

        try {
            List<Payment> payments = paymentRepository.findAllById(paymentIds);
            for (Payment payment : payments) {
                payment.setStatus(newStatus);
                payment.setUpdatedAt(LocalDateTime.now());
            }
            paymentRepository.saveAll(payments);
            System.out.println(" Updated " + payments.size() + " payments to status: " + newStatus);
            return true;
        } catch (Exception e) {
            System.out.println(" Error bulk updating payment status: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public int bulkRefund(List<Long> paymentIds, String reason) {
        System.out.println("=== BULK PROCESSING REFUNDS ===");
        System.out.println("Payment IDs: " + paymentIds);
        System.out.println("Reason: " + reason);

        int refundedCount = 0;
        try {
            List<Payment> payments = paymentRepository.findAllById(paymentIds);
            for (Payment payment : payments) {
                if (payment.canBeRefunded()) {
                    payment.setStatus("REFUNDED");
                    payment.setRefundDate(LocalDateTime.now());
                    payment.setRefundReason(reason);
                    payment.setUpdatedAt(LocalDateTime.now());
                    refundedCount++;
                }
            }
            paymentRepository.saveAll(payments);
            System.out.println(" Refunded " + refundedCount + " payments");
        } catch (Exception e) {
            System.out.println(" Error bulk processing refunds: " + e.getMessage());
            e.printStackTrace();
        }
        return refundedCount;
    }

    //  ANALYTICS


    public PaymentAnalytics getPaymentAnalytics(String period) {
        System.out.println("=== GETTING PAYMENT ANALYTICS ===");
        System.out.println("Period: " + period);

        // For now, return basic analytics - in a real application, you would implement complex analytics
        Double totalRevenue = paymentRepository.getTotalRevenue();
        Long totalPayments = paymentRepository.count();
        Long completedPayments = paymentRepository.countByStatus("COMPLETED");
        Long failedPayments = paymentRepository.countByStatus("FAILED");
        Long pendingPayments = paymentRepository.countByStatus("PENDING");
        Long refundedPayments = paymentRepository.countByStatus("REFUNDED");

        return new PaymentAnalytics(
                totalRevenue != null ? totalRevenue : 0.0,
                totalPayments,
                completedPayments,
                failedPayments,
                pendingPayments,
                refundedPayments,
                period
        );
    }


    public String generateRevenueReport(String startDate, String endDate) {
        System.out.println("=== GENERATING REVENUE REPORT ===");
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);

        // For now, return a simple text report - in a real application, you would generate a proper report
        Double totalRevenue = paymentRepository.getTotalRevenue();
        Long totalPayments = paymentRepository.count();
        
        return String.format("Revenue Report\n" +
                "==============\n" +
                "Total Revenue: $%.2f\n" +
                "Total Payments: %d\n" +
                "Average Payment: $%.2f\n" +
                "Generated: %s",
                totalRevenue != null ? totalRevenue : 0.0,
                totalPayments,
                totalPayments > 0 ? (totalRevenue != null ? totalRevenue / totalPayments : 0.0) : 0.0,
                LocalDateTime.now().toString());
    }

    //  EXPORT FUNCTIONALITY

    public void exportPaymentsToCSV(String status, String startDate, String endDate, 
                                   jakarta.servlet.http.HttpServletResponse response) throws IOException {
        System.out.println("=== EXPORTING PAYMENTS TO CSV ===");

        List<Payment> payments = paymentRepository.findAll();
        
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=payments.csv");
        
        try (java.io.PrintWriter writer = response.getWriter()) {
            writer.println("ID,Order ID,Transaction ID,Payment Method,Amount,Status,Payment Date,Refund Date,Refund Reason");
            
            for (Payment payment : payments) {
                writer.printf("%d,%d,%s,%s,%.2f,%s,%s,%s,%s%n",
                        payment.getId(),
                        payment.getOrder().getId(),
                        payment.getTransactionId() != null ? payment.getTransactionId() : "",
                        payment.getPaymentMethod(),
                        payment.getAmount(),
                        payment.getStatus(),
                        payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : "",
                        payment.getRefundDate() != null ? payment.getRefundDate().toString() : "",
                        payment.getRefundReason() != null ? payment.getRefundReason() : ""
                );
            }
        }
    }


    public void exportPaymentsToPDF(String status, String startDate, String endDate,
                                   jakarta.servlet.http.HttpServletResponse response) throws IOException {
        System.out.println("=== EXPORTING PAYMENTS TO PDF ===");

        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment; filename=payments.txt");
        
        try (java.io.PrintWriter writer = response.getWriter()) {
            writer.println("Payment Report");
            writer.println("==============");
            writer.println("Generated: " + LocalDateTime.now());
            writer.println();
            
            List<Payment> payments = paymentRepository.findAll();
            for (Payment payment : payments) {
                writer.printf("Payment ID: %d\n", payment.getId());
                writer.printf("Order ID: %d\n", payment.getOrder().getId());
                writer.printf("Amount: $%.2f\n", payment.getAmount());
                writer.printf("Status: %s\n", payment.getStatus());
                writer.println("---");
            }
        }
    }

    // VALIDATION

    public boolean validatePayment(Long paymentId) {
        System.out.println("=== VALIDATING PAYMENT ===");
        System.out.println("Payment ID: " + paymentId);

        try {
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                // Basic validation - check if payment has required fields
                return payment.getOrder() != null && 
                       payment.getAmount() != null && 
                       payment.getAmount() > 0 &&
                       payment.getStatus() != null;
            }
            return false;
        } catch (Exception e) {
            System.out.println(" Error validating payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    //  ANALYTICS DTO

    public static class PaymentAnalytics {
        private final Double totalRevenue;
        private final Long totalPayments;
        private final Long completedPayments;
        private final Long failedPayments;
        private final Long pendingPayments;
        private final Long refundedPayments;
        private final String period;

        public PaymentAnalytics(Double totalRevenue, Long totalPayments, Long completedPayments,
                               Long failedPayments, Long pendingPayments, Long refundedPayments, String period) {
            this.totalRevenue = totalRevenue;
            this.totalPayments = totalPayments;
            this.completedPayments = completedPayments;
            this.failedPayments = failedPayments;
            this.pendingPayments = pendingPayments;
            this.refundedPayments = refundedPayments;
            this.period = period;
        }

        // Getters
        public Double getTotalRevenue() { return totalRevenue; }
        public Long getTotalPayments() { return totalPayments; }
        public Long getCompletedPayments() { return completedPayments; }
        public Long getFailedPayments() { return failedPayments; }
        public Long getPendingPayments() { return pendingPayments; }
        public Long getRefundedPayments() { return refundedPayments; }
        public String getPeriod() { return period; }

        public Double getSuccessRate() {
            if (totalPayments == 0) return 0.0;
            return (completedPayments.doubleValue() / totalPayments.doubleValue()) * 100;
        }

        public Double getFailureRate() {
            if (totalPayments == 0) return 0.0;
            return (failedPayments.doubleValue() / totalPayments.doubleValue()) * 100;
        }
    }

    public static class PaymentStatistics {
        private final Double totalRevenue;
        private final Long totalPayments;
        private final Long completedPayments;
        private final Long failedPayments;
        private final Long pendingPayments;
        private final Long refundedPayments;

        public PaymentStatistics(Double totalRevenue, Long totalPayments, Long completedPayments,
                                 Long failedPayments, Long pendingPayments, Long refundedPayments) {
            this.totalRevenue = totalRevenue;
            this.totalPayments = totalPayments;
            this.completedPayments = completedPayments;
            this.failedPayments = failedPayments;
            this.pendingPayments = pendingPayments;
            this.refundedPayments = refundedPayments;
        }

        // Getters
        public Double getTotalRevenue() { return totalRevenue; }
        public Long getTotalPayments() { return totalPayments; }
        public Long getCompletedPayments() { return completedPayments; }
        public Long getFailedPayments() { return failedPayments; }
        public Long getPendingPayments() { return pendingPayments; }
        public Long getRefundedPayments() { return refundedPayments; }

        public Double getSuccessRate() {
            if (totalPayments == 0) return 0.0;
            return (completedPayments.doubleValue() / totalPayments.doubleValue()) * 100;
        }

        public Double getFailureRate() {
            if (totalPayments == 0) return 0.0;
            return (failedPayments.doubleValue() / totalPayments.doubleValue()) * 100;
        }
    }
}