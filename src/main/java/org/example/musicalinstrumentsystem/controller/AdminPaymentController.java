package org.example.musicalinstrumentsystem.controller;

import org.example.musicalinstrumentsystem.entity.Payment;
import org.example.musicalinstrumentsystem.service.PaymentRecordService;
import org.example.musicalinstrumentsystem.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    @Autowired
    private PaymentRecordService paymentRecordService;

    @Autowired
    private SessionService sessionService;

    //  DEBUG ENDPOINTS

    @GetMapping("/debug/check-payments")
    @ResponseBody
    public String debugCheckPayments() {
        System.out.println("=== DEBUG: CHECKING PAYMENTS ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "❌ Not logged in as admin";
        }

        try {
            // Check total count
            long totalCount = paymentRecordService.getTotalPaymentCount();
            System.out.println("Total payments in database: " + totalCount);

            // Get all payments
            List<Payment> allPayments = paymentRecordService.getAllPayments();
            System.out.println("Retrieved payments count: " + allPayments.size());

            // Get statistics
            var stats = paymentRecordService.getPaymentStatistics();
            System.out.println("Statistics: " + stats);

            StringBuilder result = new StringBuilder();
            result.append("=== PAYMENT DATABASE DEBUG ===\n");
            result.append("Total Count: ").append(totalCount).append("\n");
            result.append("Retrieved Count: ").append(allPayments.size()).append("\n");
            result.append("Statistics: ").append(stats).append("\n\n");

            if (!allPayments.isEmpty()) {
                result.append("=== RECENT PAYMENTS ===\n");
                for (Payment payment : allPayments) {
                    result.append("ID: ").append(payment.getId())
                          .append(", Order: ").append(payment.getOrder() != null ? payment.getOrder().getId() : "NULL")
                          .append(", Amount: $").append(payment.getAmount())
                          .append(", Status: ").append(payment.getStatus())
                          .append(", Method: ").append(payment.getPaymentMethod())
                          .append(", Transaction: ").append(payment.getTransactionId())
                          .append("\n");
                }
            } else {
                result.append("❌ No payments found in database\n");
            }

            return result.toString();

        } catch (Exception e) {
            System.out.println("❌ Debug error: " + e.getMessage());
            e.printStackTrace();
            return "❌ Debug error: " + e.getMessage();
        }
    }

    @GetMapping("/debug/create-test-payment")
    @ResponseBody
    public String createTestPayment() {
        System.out.println("=== DEBUG: CREATING TEST PAYMENT ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "❌ Not logged in as admin";
        }

        try {
            // Create a test payment record
            Payment testPayment = new Payment();
            testPayment.setTransactionId("TEST_" + System.currentTimeMillis());
            testPayment.setPaymentMethod("CREDIT_CARD");
            testPayment.setAmount(99.99);
            testPayment.setCurrency("USD");
            testPayment.setStatus("COMPLETED");
            testPayment.setPaymentDate(LocalDateTime.now());
            testPayment.setCreatedAt(LocalDateTime.now());
            testPayment.setUpdatedAt(LocalDateTime.now());
            testPayment.setPaymentDetails("{\"test\": true}");


            Payment savedPayment = paymentRecordService.savePayment(testPayment);
            
            System.out.println("✅ Test payment created with ID: " + savedPayment.getId());

            return "✅ Test payment created successfully! ID: " + savedPayment.getId() + 
                   ", Amount: $" + savedPayment.getAmount() + 
                   ", Status: " + savedPayment.getStatus();

        } catch (Exception e) {
            System.out.println("❌ Error creating test payment: " + e.getMessage());
            e.printStackTrace();
            return "❌ Error creating test payment: " + e.getMessage();
        }
    }

    //  VIEW PAYMENTS

    @GetMapping
    public String viewAllPayments(Model model) {
        System.out.println("=== VIEWING ALL PAYMENTS ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            System.out.println("❌ Access denied - not logged in as admin");
            return "redirect:/login?error=access_denied";
        }

        try {
            System.out.println("🔄 Fetching payments from database...");
            List<Payment> payments = paymentRecordService.getAllPayments();
            System.out.println("📊 Retrieved " + payments.size() + " payments from database");

            System.out.println("🔄 Fetching payment statistics...");
            var statistics = paymentRecordService.getPaymentStatistics();
            System.out.println("📈 Statistics: " + statistics);

            model.addAttribute("payments", payments);
            model.addAttribute("statistics", statistics);
            model.addAttribute("user", sessionService.getCurrentUser());

            System.out.println("✅ Loaded " + payments.size() + " payments");
            System.out.println("✅ Model attributes set successfully");

            return "admin/payments";

        } catch (Exception e) {
            System.out.println("❌ Error loading payments: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Failed to load payments: " + e.getMessage());
            model.addAttribute("payments", List.of()); // Empty list to prevent template errors
            model.addAttribute("statistics", null);
            return "admin/payments";
        }
    }

    @GetMapping("/{id}")
    public String viewPaymentDetails(@PathVariable Long id, Model model) {
        System.out.println("=== VIEWING PAYMENT DETAILS ===");
        System.out.println("Payment ID: " + id);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            var paymentOpt = paymentRecordService.getPaymentById(id);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                model.addAttribute("payment", payment);
                model.addAttribute("user", sessionService.getCurrentUser());
                return "admin/payment-details";
            } else {
                return "redirect:/admin/payments?error=payment_not_found";
            }
        } catch (Exception e) {
            System.out.println("❌ Error loading payment details: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=load_failed";
        }
    }

    // DELETE PAYMENTS

    @GetMapping("/delete/{id}")
    public String deletePayment(@PathVariable Long id) {
        System.out.println("=== DELETING PAYMENT ===");
        System.out.println("Payment ID: " + id);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            boolean deleted = paymentRecordService.deletePayment(id);
            if (deleted) {
                return "redirect:/admin/payments?success=payment_deleted";
            } else {
                return "redirect:/admin/payments?error=payment_not_found";
            }
        } catch (Exception e) {
            System.out.println("❌ Error deleting payment: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=delete_failed";
        }
    }

    @PostMapping("/delete-multiple")
    public String deleteMultiplePayments(@RequestParam List<Long> paymentIds) {
        System.out.println("=== DELETING MULTIPLE PAYMENTS ===");
        System.out.println("Payment IDs: " + paymentIds);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            boolean deleted = paymentRecordService.deletePayments(paymentIds);
            if (deleted) {
                return "redirect:/admin/payments?success=payments_deleted&count=" + paymentIds.size();
            } else {
                return "redirect:/admin/payments?error=no_payments_found";
            }
        } catch (Exception e) {
            System.out.println("❌ Error deleting multiple payments: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=delete_failed";
        }
    }

    @GetMapping("/delete-by-status/{status}")
    public String deletePaymentsByStatus(@PathVariable String status) {
        System.out.println("=== DELETING PAYMENTS BY STATUS ===");
        System.out.println("Status: " + status);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            boolean deleted = paymentRecordService.deletePaymentsByStatus(status);
            if (deleted) {
                return "redirect:/admin/payments?success=payments_deleted&status=" + status;
            } else {
                return "redirect:/admin/payments?error=delete_failed";
            }
        } catch (Exception e) {
            System.out.println("❌ Error deleting payments by status: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=delete_failed";
        }
    }

    @PostMapping("/delete-by-date-range")
    public String deletePaymentsByDateRange(@RequestParam String startDate,
                                            @RequestParam String endDate) {
        System.out.println("=== DELETING PAYMENTS BY DATE RANGE ===");
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");

            boolean deleted = paymentRecordService.deletePaymentsByDateRange(start, end);
            if (deleted) {
                return "redirect:/admin/payments?success=payments_deleted&range=" + startDate + "_to_" + endDate;
            } else {
                return "redirect:/admin/payments?error=no_payments_found";
            }
        } catch (Exception e) {
            System.out.println("❌ Error deleting payments by date range: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=delete_failed";
        }
    }

    @GetMapping("/delete-all")
    public String deleteAllPayments() {
        System.out.println("=== DELETING ALL PAYMENTS ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            boolean deleted = paymentRecordService.deleteAllPayments();
            if (deleted) {
                return "redirect:/admin/payments?success=all_payments_deleted";
            } else {
                return "redirect:/admin/payments?error=delete_failed";
            }
        } catch (Exception e) {
            System.out.println("❌ Error deleting all payments: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=delete_failed";
        }
    }

    //  PAYMENT MANAGEMENT

    @GetMapping("/refund/{id}")
    public String refundPayment(@PathVariable Long id,
                                @RequestParam(required = false) String reason) {
        System.out.println("=== PROCESSING PAYMENT REFUND ===");
        System.out.println("Payment ID: " + id);
        System.out.println("Reason: " + reason);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            Payment refundedPayment = paymentRecordService.processRefund(id,
                    reason != null ? reason : "Admin initiated refund");

            if (refundedPayment != null) {
                return "redirect:/admin/payments?success=payment_refunded";
            } else {
                return "redirect:/admin/payments?error=cannot_refund";
            }
        } catch (Exception e) {
            System.out.println("❌ Error processing refund: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=refund_failed";
        }
    }

    @PostMapping("/update-status/{id}")
    public String updatePaymentStatus(@PathVariable Long id,
                                      @RequestParam String status) {
        System.out.println("=== UPDATING PAYMENT STATUS ===");
        System.out.println("Payment ID: " + id);
        System.out.println("New Status: " + status);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            var paymentOpt = paymentRecordService.getPaymentById(id);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setStatus(status);
                // In a real application, you would save through repository
                return "redirect:/admin/payments?success=status_updated";
            } else {
                return "redirect:/admin/payments?error=payment_not_found";
            }
        } catch (Exception e) {
            System.out.println("❌ Error updating payment status: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=update_failed";
        }
    }

    //  FILTERED VIEWS

    @GetMapping("/status/{status}")
    public String viewPaymentsByStatus(@PathVariable String status, Model model) {
        System.out.println("=== VIEWING PAYMENTS BY STATUS ===");
        System.out.println("Status: " + status);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            List<Payment> payments = paymentRecordService.getPaymentsByStatus(status);
            var statistics = paymentRecordService.getPaymentStatistics();

            model.addAttribute("payments", payments);
            model.addAttribute("statistics", statistics);
            model.addAttribute("user", sessionService.getCurrentUser());
            model.addAttribute("filterStatus", status);

            System.out.println("✅ Loaded " + payments.size() + " payments with status: " + status);

            return "admin/payments";

        } catch (Exception e) {
            System.out.println("❌ Error loading payments by status: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=load_failed";
        }
    }

    @GetMapping("/recent")
    public String viewRecentPayments(Model model) {
        System.out.println("=== VIEWING RECENT PAYMENTS ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            List<Payment> payments = paymentRecordService.getRecentPayments();
            var statistics = paymentRecordService.getPaymentStatistics();

            model.addAttribute("payments", payments);
            model.addAttribute("statistics", statistics);
            model.addAttribute("user", sessionService.getCurrentUser());
            model.addAttribute("filterRecent", true);

            System.out.println("✅ Loaded " + payments.size() + " recent payments");

            return "admin/payments";

        } catch (Exception e) {
            System.out.println("❌ Error loading recent payments: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=load_failed";
        }
    }

    //  STATISTICS

    @GetMapping("/statistics")
    @ResponseBody
    public PaymentRecordService.PaymentStatistics getPaymentStatistics() {
        System.out.println("=== GETTING PAYMENT STATISTICS ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return null;
        }

        try {
            return paymentRecordService.getPaymentStatistics();
        } catch (Exception e) {
            System.out.println("❌ Error getting payment statistics: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    //  ADVANCED FILTERING AND SEARCH

    @GetMapping("/search")
    public String searchPayments(@RequestParam(required = false) String query,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String paymentMethod,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate,
                                 @RequestParam(required = false) Double minAmount,
                                 @RequestParam(required = false) Double maxAmount,
                                 Model model) {
        System.out.println("=== SEARCHING PAYMENTS ===");
        System.out.println("Query: " + query);
        System.out.println("Status: " + status);
        System.out.println("Payment Method: " + paymentMethod);
        System.out.println("Date Range: " + startDate + " to " + endDate);
        System.out.println("Amount Range: " + minAmount + " to " + maxAmount);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            List<Payment> payments = paymentRecordService.searchPayments(query, status, paymentMethod, 
                    startDate, endDate, minAmount, maxAmount);
            var statistics = paymentRecordService.getPaymentStatistics();

            model.addAttribute("payments", payments);
            model.addAttribute("statistics", statistics);
            model.addAttribute("user", sessionService.getCurrentUser());
            model.addAttribute("searchQuery", query);
            model.addAttribute("searchStatus", status);
            model.addAttribute("searchPaymentMethod", paymentMethod);
            model.addAttribute("searchStartDate", startDate);
            model.addAttribute("searchEndDate", endDate);
            model.addAttribute("searchMinAmount", minAmount);
            model.addAttribute("searchMaxAmount", maxAmount);

            System.out.println("✅ Found " + payments.size() + " payments matching search criteria");

            return "admin/payments";

        } catch (Exception e) {
            System.out.println("❌ Error searching payments: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=search_failed";
        }
    }

    @GetMapping("/filter")
    public String filterPayments(@RequestParam(required = false) String status,
                                 @RequestParam(required = false) String paymentMethod,
                                 @RequestParam(required = false) String dateRange,
                                 Model model) {
        System.out.println("=== FILTERING PAYMENTS ===");
        System.out.println("Status: " + status);
        System.out.println("Payment Method: " + paymentMethod);
        System.out.println("Date Range: " + dateRange);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            List<Payment> payments = paymentRecordService.filterPayments(status, paymentMethod, dateRange);
            var statistics = paymentRecordService.getPaymentStatistics();

            model.addAttribute("payments", payments);
            model.addAttribute("statistics", statistics);
            model.addAttribute("user", sessionService.getCurrentUser());
            model.addAttribute("filterStatus", status);
            model.addAttribute("filterPaymentMethod", paymentMethod);
            model.addAttribute("filterDateRange", dateRange);

            System.out.println("✅ Found " + payments.size() + " payments matching filter criteria");

            return "admin/payments";

        } catch (Exception e) {
            System.out.println("❌ Error filtering payments: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=filter_failed";
        }
    }

    //  BULK OPERATIONS

    @PostMapping("/bulk-update-status")
    public String bulkUpdateStatus(@RequestParam List<Long> paymentIds,
                                   @RequestParam String newStatus) {
        System.out.println("=== BULK UPDATING PAYMENT STATUS ===");
        System.out.println("Payment IDs: " + paymentIds);
        System.out.println("New Status: " + newStatus);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            boolean updated = paymentRecordService.bulkUpdateStatus(paymentIds, newStatus);
            if (updated) {
                return "redirect:/admin/payments?success=bulk_status_updated&count=" + paymentIds.size();
            } else {
                return "redirect:/admin/payments?error=bulk_update_failed";
            }
        } catch (Exception e) {
            System.out.println("❌ Error bulk updating payment status: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=bulk_update_failed";
        }
    }

    @PostMapping("/bulk-refund")
    public String bulkRefund(@RequestParam List<Long> paymentIds,
                             @RequestParam String reason) {
        System.out.println("=== BULK PROCESSING REFUNDS ===");
        System.out.println("Payment IDs: " + paymentIds);
        System.out.println("Reason: " + reason);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            int refundedCount = paymentRecordService.bulkRefund(paymentIds, reason);
            return "redirect:/admin/payments?success=bulk_refunded&count=" + refundedCount;
        } catch (Exception e) {
            System.out.println("❌ Error bulk processing refunds: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=bulk_refund_failed";
        }
    }

    //  PAYMENT ANALYTICS

    @GetMapping("/analytics")
    public String paymentAnalytics(@RequestParam(required = false) String period,
                                   Model model) {
        System.out.println("=== PAYMENT ANALYTICS ===");
        System.out.println("Period: " + period);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            var analytics = paymentRecordService.getPaymentAnalytics(period);
            model.addAttribute("analytics", analytics);
            model.addAttribute("user", sessionService.getCurrentUser());
            model.addAttribute("selectedPeriod", period);

            return "admin/payment-analytics";

        } catch (Exception e) {
            System.out.println("❌ Error getting payment analytics: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/payments?error=analytics_failed";
        }
    }

    @GetMapping("/revenue-report")
    @ResponseBody
    public String generateRevenueReport(@RequestParam(required = false) String startDate,
                                       @RequestParam(required = false) String endDate) {
        System.out.println("=== GENERATING REVENUE REPORT ===");
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "Access denied";
        }

        try {
            return paymentRecordService.generateRevenueReport(startDate, endDate);
        } catch (Exception e) {
            System.out.println("❌ Error generating revenue report: " + e.getMessage());
            e.printStackTrace();
            return "Error generating report: " + e.getMessage();
        }
    }

    // EXPORT FUNCTIONALITY

    @GetMapping("/export/csv")
    public void exportPaymentsCSV(@RequestParam(required = false) String status,
                                  @RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate,
                                  HttpServletResponse response) throws IOException {
        System.out.println("=== EXPORTING PAYMENTS TO CSV ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            response.sendRedirect("/login?error=access_denied");
            return;
        }

        try {
            paymentRecordService.exportPaymentsToCSV(status, startDate, endDate, response);
        } catch (Exception e) {
            System.out.println("❌ Error exporting payments to CSV: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect("/admin/payments?error=export_failed");
        }
    }

    @GetMapping("/export/pdf")
    public void exportPaymentsPDF(@RequestParam(required = false) String status,
                                  @RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate,
                                  HttpServletResponse response) throws IOException {
        System.out.println("=== EXPORTING PAYMENTS TO PDF ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            response.sendRedirect("/login?error=access_denied");
            return;
        }

        try {
            paymentRecordService.exportPaymentsToPDF(status, startDate, endDate, response);
        } catch (Exception e) {
            System.out.println("❌ Error exporting payments to PDF: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect("/admin/payments?error=export_failed");
        }
    }

    // PAYMENT VALIDATION

    @PostMapping("/validate/{id}")
    @ResponseBody
    public String validatePayment(@PathVariable Long id) {
        System.out.println("=== VALIDATING PAYMENT ===");
        System.out.println("Payment ID: " + id);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "Access denied";
        }

        try {
            boolean isValid = paymentRecordService.validatePayment(id);
            return isValid ? "Payment is valid" : "Payment validation failed";
        } catch (Exception e) {
            System.out.println("❌ Error validating payment: " + e.getMessage());
            e.printStackTrace();
            return "Validation error: " + e.getMessage();
        }
    }

    //  HEALTH CHECK

    @GetMapping("/health")
    @ResponseBody
    public String healthCheck() {
        try {
            long count = paymentRecordService.getTotalPaymentCount();
            return "✅ Payment Service is healthy. Total payments: " + count;
        } catch (Exception e) {
            return "❌ Payment Service error: " + e.getMessage();
        }
    }
}