package org.example.musicalinstrumentsystem.payment.strategy;

import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.payment.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PayPalPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult processPayment(Order order, User user, PaymentDetails paymentDetails) {
        try {
            System.out.println("=== PROCESSING PAYPAL PAYMENT ===");
            System.out.println("Order ID: " + order.getId());
            System.out.println("Amount: $" + order.getTotalAmount());
            System.out.println("User: " + user.getEmail());

            // Validate payment details
            if (paymentDetails == null || paymentDetails.getPaymentData() == null) {
                return PaymentResult.failure("INVALID_DATA", "PayPal payment data is missing");
            }

            String email = paymentDetails.getPaymentData("email");
            String password = paymentDetails.getPaymentData("password"); // In real app, use OAuth

            // Validate required fields
            if (email == null || password == null) {
                return PaymentResult.failure("MISSING_FIELDS", "PayPal email and password are required");
            }

            // Validate email format
            if (!isValidEmail(email)) {
                return PaymentResult.failure("INVALID_EMAIL", "Invalid PayPal email address");
            }

            // PayPal authentication
            System.out.println("🔐 Authenticating with PayPal...");

            // API call delay
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if ("fail@test.com".equalsIgnoreCase(email)) {
                return PaymentResult.failure("AUTH_FAILED", "PayPal authentication failed");
            }

            // transaction ID
            String transactionId = "PP_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            System.out.println("✅ PayPal payment processed successfully");
            System.out.println("📝 Transaction ID: " + transactionId);
            System.out.println("📧 PayPal Email: " + email);

            PaymentResult result = PaymentResult.success(transactionId, "PayPal payment processed successfully");
            result.setAmount(order.getTotalAmount());

            return result;

        } catch (Exception e) {
            System.out.println("❌ PayPal payment failed: " + e.getMessage());
            return PaymentResult.failure("PROCESSING_ERROR", "PayPal payment failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.PAYPAL.equals(paymentMethod);
    }

    @Override
    public String getPaymentMethodName() {
        return "PayPal";
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}