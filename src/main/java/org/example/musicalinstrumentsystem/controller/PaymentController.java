package org.example.musicalinstrumentsystem.controller;

import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.Payment;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.payment.PaymentContext;
import org.example.musicalinstrumentsystem.payment.PaymentDetails;
import org.example.musicalinstrumentsystem.payment.PaymentMethod;
import org.example.musicalinstrumentsystem.payment.PaymentResult;
import org.example.musicalinstrumentsystem.service.OrderService;
import org.example.musicalinstrumentsystem.service.PaymentRecordService;
import org.example.musicalinstrumentsystem.service.PaymentService;
import org.example.musicalinstrumentsystem.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private PaymentRecordService paymentRecordService;

    @Autowired
    private PaymentContext paymentContext;

    // PAYMENT PAGES

    @GetMapping("/order/{orderId}")
    public String showPaymentPage(@PathVariable Long orderId,
                                  @RequestParam(required = false) String error,
                                  @RequestParam(required = false) String message,
                                  Model model) {
        System.out.println("=== SHOWING PAYMENT PAGE ===");
        System.out.println("Order ID: " + orderId);
        System.out.println("Error: " + error);
        System.out.println("Message: " + message);
        System.out.println("Request URL: /payment/order/" + orderId);

        if (!sessionService.isLoggedIn()) {
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();

        try {
            var orderOpt = orderService.getOrderById(orderId);
            if (orderOpt.isEmpty()) {
                System.out.println(" Order not found: " + orderId);
                return "redirect:/buyer/orders?error=order_not_found";
            }

            Order order = orderOpt.get();

            // Check if order belongs to user
            if (!order.getBuyer().getId().equals(currentUser.getId())) {
                System.out.println(" Order access denied for user: " + currentUser.getEmail());
                return "redirect:/buyer/orders?error=access_denied";
            }

            // Check if order is already paid
            if ("COMPLETED".equals(order.getPaymentStatus())) {
                System.out.println(" Order already paid, redirecting to success");
                return "redirect:/payment/success/" + orderId + "?transactionId=" + order.getTransactionId();
            }

            System.out.println(" Loading payment page for order: " + orderId);
            System.out.println(" Order Amount: $" + order.getTotalAmount());
            System.out.println(" Order Status: " + order.getStatus());
            System.out.println(" Payment Status: " + order.getPaymentStatus());

            model.addAttribute("user", currentUser);
            model.addAttribute("order", order);
            model.addAttribute("paymentMethods", paymentService.getSupportedPaymentMethods());
            model.addAttribute("defaultMethod", PaymentMethod.CREDIT_CARD);

            // Add error message if present
            if (error != null) {
                model.addAttribute("error", error);
                model.addAttribute("errorMessage", message);
            }

            return "payment/payment-page";

        } catch (Exception e) {
            System.out.println(" Error showing payment page: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/buyer/orders?error=payment_error";
        }
    }

    @PostMapping("/process/{orderId}")
    public String processPayment(@PathVariable Long orderId,
                                 @RequestParam String paymentMethod,
                                 @RequestParam Map<String, String> paymentData,
                                 Model model) {
        System.out.println("=== PROCESSING PAYMENT REQUEST ===");
        System.out.println("Order ID: " + orderId);
        System.out.println("Payment Method: " + paymentMethod);
        System.out.println("Payment Data: " + paymentData);
        System.out.println("Request URL: /payment/process/" + orderId);

        if (!sessionService.isLoggedIn()) {
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();

        try {
            var orderOpt = orderService.getOrderById(orderId);
            if (orderOpt.isEmpty()) {
                System.out.println(" Order not found: " + orderId);
                return "redirect:/buyer/orders?error=order_not_found";
            }

            Order order = orderOpt.get();

            // Check if order belongs to user
            if (!order.getBuyer().getId().equals(currentUser.getId())) {
                System.out.println(" Order access denied for user: " + currentUser.getEmail());
                return "redirect:/buyer/orders?error=access_denied";
            }

            // Check if order is already paid
            if ("COMPLETED".equals(order.getPaymentStatus())) {
                System.out.println(" Order already paid, redirecting to success");
                return "redirect:/payment/success/" + orderId + "?transactionId=" + order.getTransactionId();
            }

            System.out.println(" Creating payment details...");
            System.out.println("Payment Method String: " + paymentMethod);
            System.out.println("Payment Data Map: " + paymentData);
            
            // Create payment details
            PaymentDetails paymentDetails = new PaymentDetails();
            paymentDetails.setPaymentMethod(PaymentMethod.valueOf(paymentMethod));
            paymentDetails.setPaymentData(new HashMap<>(paymentData));
            
            System.out.println("Payment Details Created:");
            System.out.println("  - Method: " + paymentDetails.getPaymentMethod());
            System.out.println("  - Data: " + paymentDetails.getPaymentData());

            System.out.println(" Processing payment...");
            // Process payment using simplified approach
            PaymentResult result = processPaymentSimple(order, currentUser, paymentDetails);

            System.out.println(" Payment Result Analysis:");
            System.out.println("   - Success: " + result.isSuccess());
            System.out.println("   - Transaction ID: " + result.getTransactionId());
            System.out.println("   - Message: " + result.getMessage());

            if (result.isSuccess()) {
                System.out.println(" Payment successful, redirecting to success page");
                return "redirect:/payment/success/" + orderId + "?transactionId=" + result.getTransactionId();
            } else {
                System.out.println(" Payment failed: " + result.getMessage());
                // Return to payment page with error
                String encodedMessage = URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8);
                return "redirect:/payment/order/" + orderId + "?error=payment_failed&message=" + encodedMessage;
            }

        } catch (Exception e) {
            System.out.println(" Payment processing error: " + e.getMessage());
            e.printStackTrace();
            String encodedMessage = URLEncoder.encode("Payment processing error: " + e.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/payment/order/" + orderId + "?error=processing_error&message=" + encodedMessage;
        }
    }



    @GetMapping("/success/{orderId}")
    public String paymentSuccess(@PathVariable Long orderId,
                                 @RequestParam String transactionId,
                                 Model model) {
        System.out.println("=== PAYMENT SUCCESS PAGE ===");
        System.out.println("Order ID: " + orderId);
        System.out.println("Transaction ID: " + transactionId);

        if (!sessionService.isLoggedIn()) {
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();

        try {
            var orderOpt = orderService.getOrderById(orderId);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();

                System.out.println(" Loading success page for order:");
                System.out.println("   - Order Status: " + order.getStatus());
                System.out.println("   - Payment Status: " + order.getPaymentStatus());
                System.out.println("   - Transaction ID: " + order.getTransactionId());

                model.addAttribute("user", currentUser);
                model.addAttribute("order", order);
                model.addAttribute("transactionId", transactionId);
                return "payment/payment-success";
            } else {
                System.out.println(" Order not found for success page: " + orderId);
                return "redirect:/buyer/orders?error=order_not_found";
            }
        } catch (Exception e) {
            System.out.println(" Error showing payment success: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/buyer/orders?error=payment_success_error";
        }
    }




    @GetMapping("/cancel/{orderId}")
    public String paymentCancel(@PathVariable Long orderId, Model model) {
        System.out.println("=== PAYMENT CANCELLED ===");
        System.out.println("Order ID: " + orderId);

        if (!sessionService.isLoggedIn()) {
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();
        model.addAttribute("user", currentUser);
        model.addAttribute("orderId", orderId);

        return "payment/payment-cancel";
    }

    @GetMapping("/api/methods")
    @ResponseBody
    public Map<String, Object> getPaymentMethods() {
        try {
            var methods = paymentService.getSupportedPaymentMethods();
            return Map.of(
                    "success", true,
                    "methods", methods,
                    "count", methods.size()
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }


    @PostMapping("/api/validate")
    @ResponseBody
    public Map<String, Object> validatePayment(@RequestBody PaymentDetails paymentDetails) {
        try {
            boolean isValid = paymentService.validatePaymentDetails(paymentDetails);
            return Map.of(
                    "success", true,
                    "valid", isValid
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }


    private PaymentResult processPaymentSimple(Order order, User user, PaymentDetails paymentDetails) {
        System.out.println("=== SIMPLIFIED PAYMENT PROCESSING ===");
        System.out.println("Order ID: " + order.getId());
        System.out.println("User: " + user.getEmail());
        System.out.println("Payment Method: " + paymentDetails.getPaymentMethod());

        try {
            // Validate order status
            if (!"PENDING".equals(order.getStatus())) {
                System.out.println(" Invalid order status: " + order.getStatus());
                return PaymentResult.failure("INVALID_ORDER_STATUS",
                        "Cannot process payment for order with status: " + order.getStatus());
            }

            // Check if already paid
            if ("COMPLETED".equals(order.getPaymentStatus())) {
                System.out.println(" Order already paid");
                return PaymentResult.failure("ALREADY_PAID", "Order has already been paid");
            }

            // Process payment using strategy pattern
            System.out.println(" Processing payment with strategy...");
            PaymentResult paymentResult = paymentContext.processPayment(order, user, paymentDetails);

            System.out.println(" Payment Result: " + (paymentResult.isSuccess() ? "SUCCESS" : "FAILED"));
            System.out.println(" Transaction ID: " + paymentResult.getTransactionId());
            System.out.println(" Message: " + paymentResult.getMessage());

            if (paymentResult.isSuccess()) {
                // Create payment record
                System.out.println(" Creating payment record...");
                Payment paymentRecord = new Payment();
                
                // Set all required fields explicitly
                paymentRecord.setOrder(order);
                paymentRecord.setPaymentMethod(paymentDetails.getPaymentMethod().getDisplayName());
                paymentRecord.setAmount(order.getTotalAmount());
                paymentRecord.setCurrency("USD");
                paymentRecord.setStatus("COMPLETED");
                paymentRecord.setTransactionId(paymentResult.getTransactionId());
                paymentRecord.setPaymentDate(LocalDateTime.now());
                paymentRecord.setCreatedAt(LocalDateTime.now());
                paymentRecord.setUpdatedAt(LocalDateTime.now());
                
                // Set payment details as JSON string
                String paymentDetailsJson = "{}";
                if (paymentDetails.getPaymentData() != null && !paymentDetails.getPaymentData().isEmpty()) {
                    paymentDetailsJson = paymentDetails.getPaymentData().toString();
                }
                paymentRecord.setPaymentDetails(paymentDetailsJson);
                
                System.out.println("Payment record details before save:");
                System.out.println("  - Order ID: " + paymentRecord.getOrder().getId());
                System.out.println("  - Amount: " + paymentRecord.getAmount());
                System.out.println("  - Status: " + paymentRecord.getStatus());
                System.out.println("  - Transaction ID: " + paymentRecord.getTransactionId());
                System.out.println("  - Payment Method: " + paymentRecord.getPaymentMethod());
                System.out.println("  - Payment Date: " + paymentRecord.getPaymentDate());
                System.out.println("  - Created At: " + paymentRecord.getCreatedAt());
                System.out.println("  - Updated At: " + paymentRecord.getUpdatedAt());

                Payment savedPayment = paymentRecordService.savePayment(paymentRecord);
                System.out.println(" Payment record created with ID: " + savedPayment.getId());

                // Update order
                System.out.println(" Updating order...");
                order.setPaymentStatus("COMPLETED");
                order.setTransactionId(paymentResult.getTransactionId());
                order.setPaymentMethod(paymentDetails.getPaymentMethod().getDisplayName());
                order.setPaymentDate(LocalDateTime.now());
                order.setStatus("CONFIRMED");

                Order savedOrder = orderService.saveOrder(order);
                System.out.println(" Order updated successfully");

                System.out.println(" Payment processing completed successfully");
                System.out.println(" Final Order Status: " + savedOrder.getStatus());
                System.out.println(" Final Payment Status: " + savedOrder.getPaymentStatus());
                System.out.println(" Transaction ID: " + savedOrder.getTransactionId());
                System.out.println(" Payment Record ID: " + savedPayment.getId());
            } else {
                // Create failed payment record
                System.out.println(" Creating failed payment record...");
                Payment paymentRecord = new Payment();
                
                // Set all required fields explicitly
                paymentRecord.setOrder(order);
                paymentRecord.setPaymentMethod(paymentDetails.getPaymentMethod().getDisplayName());
                paymentRecord.setAmount(order.getTotalAmount());
                paymentRecord.setCurrency("USD");
                paymentRecord.setStatus("FAILED");
                paymentRecord.setTransactionId("FAILED_" + System.currentTimeMillis());
                paymentRecord.setPaymentDate(LocalDateTime.now());
                paymentRecord.setCreatedAt(LocalDateTime.now());
                paymentRecord.setUpdatedAt(LocalDateTime.now());
                
                // Set payment details as JSON string
                String paymentDetailsJson = "{}";
                if (paymentDetails.getPaymentData() != null && !paymentDetails.getPaymentData().isEmpty()) {
                    paymentDetailsJson = paymentDetails.getPaymentData().toString();
                }
                paymentRecord.setPaymentDetails(paymentDetailsJson);

                paymentRecordService.savePayment(paymentRecord);
                System.out.println(" Failed payment record created");

                // Update order to failed
                order.setPaymentStatus("FAILED");
                orderService.saveOrder(order);
                System.out.println(" Order marked as failed");
            }

            return paymentResult;

        } catch (Exception e) {
            System.out.println(" Simplified payment processing error: " + e.getMessage());
            e.printStackTrace();
            return PaymentResult.failure("PAYMENT_SERVICE_ERROR", "Payment processing error: " + e.getMessage());
        }
    }



    @GetMapping("/debug/test-payment-creation/{orderId}")
    @ResponseBody
    public String testPaymentCreation(@PathVariable Long orderId) {
        System.out.println("=== TESTING PAYMENT CREATION ===");
        System.out.println("Order ID: " + orderId);

        if (!sessionService.isLoggedIn()) {
            return " Not logged in";
        }

        try {
            var orderOpt = orderService.getOrderById(orderId);
            if (orderOpt.isEmpty()) {
                return " Order not found: " + orderId;
            }

            Order order = orderOpt.get();
            System.out.println("Order found: " + order.getId() + " - Amount: $" + order.getTotalAmount());

            // Test database connection first
            System.out.println(" Testing database connection...");
            try {
                long paymentCount = paymentRecordService.getTotalPaymentCount();
                System.out.println(" Database connection OK. Total payments: " + paymentCount);
            } catch (Exception dbError) {
                System.out.println(" Database connection failed: " + dbError.getMessage());
                return " Database connection failed: " + dbError.getMessage();
            }

            // Create test payment record directly
            System.out.println(" Creating test payment record...");
            Payment testPayment = new Payment();
            testPayment.setOrder(order);
            testPayment.setPaymentMethod("CREDIT_CARD");
            testPayment.setAmount(order.getTotalAmount());
            testPayment.setCurrency("USD");
            testPayment.setStatus("PENDING");
            testPayment.setTransactionId("TEST_" + System.currentTimeMillis());
            testPayment.setPaymentDate(LocalDateTime.now());
            testPayment.setCreatedAt(LocalDateTime.now());
            testPayment.setUpdatedAt(LocalDateTime.now());
            testPayment.setPaymentDetails("{\"test\": true}");

            Payment savedPayment = paymentRecordService.savePayment(testPayment);
            System.out.println(" Test payment record created with ID: " + savedPayment.getId());

            return " Test payment creation successful! Payment ID: " + savedPayment.getId() +
                   ", Order ID: " + order.getId() + 
                   ", Amount: $" + order.getTotalAmount();

        } catch (Exception e) {
            System.out.println(" Test payment creation failed: " + e.getMessage());
            System.out.println(" Error type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            return " Test payment creation failed: " + e.getMessage();
        }
    }

    @GetMapping("/debug/database-health")
    @ResponseBody
    public String databaseHealthCheck() {
        System.out.println("=== DATABASE HEALTH CHECK ===");
        
        try {
            // Test payment repository
            long paymentCount = paymentRecordService.getTotalPaymentCount();
            System.out.println(" Payment repository OK. Total payments: " + paymentCount);
            
            // Test order repository
            long orderCount = orderService.getTotalOrderCount();
            System.out.println(" Order repository OK. Total orders: " + orderCount);
            
            return " Database health check passed! Payments: " + paymentCount + ", Orders: " + orderCount;
            
        } catch (Exception e) {
            System.out.println(" Database health check failed: " + e.getMessage());
            System.out.println(" Error type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            return " Database health check failed: " + e.getMessage();
        }
    }
}