package org.example.musicalinstrumentsystem.payment.strategy;

import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.payment.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StripePaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult processPayment(Order order, User user, PaymentDetails paymentDetails) {
        try {
            System.out.println("=== PROCESSING STRIPE PAYMENT ===");
            System.out.println("Order ID: " + order.getId());
            System.out.println("Amount: $" + order.getTotalAmount());
            System.out.println("User: " + user.getEmail());

            // Validate payment details
            if (paymentDetails == null || paymentDetails.getPaymentData() == null) {
                return PaymentResult.failure("INVALID_DATA", "Stripe payment data is missing");
            }

            String stripeToken = paymentDetails.getPaymentData("stripeToken");
            String paymentMethodId = paymentDetails.getPaymentData("paymentMethodId");

            // Validate required fields
            if (stripeToken == null && paymentMethodId == null) {
                return PaymentResult.failure("MISSING_FIELDS", "Stripe token or payment method ID is required");
            }

            // Stripe payment processing
            System.out.println("💳 Processing Stripe payment...");

            // API call delay
            try {
                Thread.sleep(1200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if ("fail_".equals(stripeToken) || "fail_".equals(paymentMethodId)) {
                return PaymentResult.failure("STRIPE_DECLINED", "Card was declined by Stripe");
            }

            // transaction ID
            String transactionId = "ST_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            System.out.println("✅ Stripe payment processed successfully");
            System.out.println("📝 Transaction ID: " + transactionId);
            System.out.println("💳 Payment Method: " + (paymentMethodId != null ? "PaymentMethod" : "Token"));

            PaymentResult result = PaymentResult.success(transactionId, "Stripe payment processed successfully");
            result.setAmount(order.getTotalAmount());

            return result;

        } catch (Exception e) {
            System.out.println("❌ Stripe payment failed: " + e.getMessage());
            return PaymentResult.failure("PROCESSING_ERROR", "Stripe payment failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.STRIPE.equals(paymentMethod);
    }

    @Override
    public String getPaymentMethodName() {
        return "Stripe";
    }
}