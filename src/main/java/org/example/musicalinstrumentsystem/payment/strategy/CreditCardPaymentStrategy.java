package org.example.musicalinstrumentsystem.payment.strategy;

import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.payment.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreditCardPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult processPayment(Order order, User user, PaymentDetails paymentDetails) {
        try {
            System.out.println("=== PROCESSING CREDIT CARD PAYMENT ===");
            System.out.println("Order ID: " + order.getId());
            System.out.println("Amount: $" + order.getTotalAmount());
            System.out.println("User: " + user.getEmail());

            // Validate payment details
            if (paymentDetails == null || paymentDetails.getPaymentData() == null) {
                return PaymentResult.failure("INVALID_DATA", "Payment data is missing");
            }

            String cardNumber = paymentDetails.getPaymentData("cardNumber");
            String expiryDate = paymentDetails.getPaymentData("expiryDate");
            String cvv = paymentDetails.getPaymentData("cvv");
            String cardHolder = paymentDetails.getPaymentData("cardHolder");

            // Validate required fields
            if (cardNumber == null || expiryDate == null || cvv == null || cardHolder == null) {
                return PaymentResult.failure("MISSING_FIELDS", "Required credit card fields are missing");
            }

            // credit card validation
            if (!isValidCardNumber(cardNumber)) {
                return PaymentResult.failure("INVALID_CARD", "Invalid credit card number");
            }

            if (!isValidExpiryDate(expiryDate)) {
                return PaymentResult.failure("INVALID_EXPIRY", "Card has expired or invalid expiry date");
            }

            // payment processing
            System.out.println("💳 Processing credit card payment...");

            // API call delay
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // transaction ID
            String transactionId = "CC_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            System.out.println("✅ Credit card payment processed successfully");
            System.out.println("📝 Transaction ID: " + transactionId);

            PaymentResult result = PaymentResult.success(transactionId, "Credit card payment processed successfully");
            result.setAmount(order.getTotalAmount());

            return result;

        } catch (Exception e) {
            System.out.println("❌ Credit card payment failed: " + e.getMessage());
            return PaymentResult.failure("PROCESSING_ERROR", "Credit card payment failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.CREDIT_CARD.equals(paymentMethod);
    }

    @Override
    public String getPaymentMethodName() {
        return "Credit Card";
    }

    // Validation methods
    private boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }

        // Remove spaces
        String cleanCardNumber = cardNumber.replaceAll("[\\s-]", "");
        
        // Check if it's 16 digits
        if (cleanCardNumber.length() != 16) {
            return false;
        }

        if (cleanCardNumber.equals("4111111111111111") || 
            cleanCardNumber.equals("4000000000000002") ||
            cleanCardNumber.equals("5555555555554444") ||
            cleanCardNumber.startsWith("4111")) {
            System.out.println("✅ Test card number accepted: " + cleanCardNumber);
            return true;
        }

        return cleanCardNumber.matches("\\d{16}");
    }

    private boolean isValidExpiryDate(String expiryDate) {
        if (expiryDate == null || !expiryDate.matches("(0[1-9]|1[0-2])/[0-9]{2}")) {
            return false;
        }

        String[] parts = expiryDate.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt("20" + parts[1]);

        // Accept any date from 2024
        if (year >= 2024) {
            System.out.println("✅ Expiry date accepted for testing: " + expiryDate);
            return true;
        }

        java.time.YearMonth expiry = java.time.YearMonth.of(year, month);
        return !expiry.isBefore(java.time.YearMonth.now());
    }
}