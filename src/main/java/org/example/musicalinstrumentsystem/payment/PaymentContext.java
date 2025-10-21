package org.example.musicalinstrumentsystem.payment;

import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentContext {

    private final List<PaymentStrategy> paymentStrategies;

    @Autowired
    public PaymentContext(List<PaymentStrategy> paymentStrategies) {
        this.paymentStrategies = paymentStrategies;
        System.out.println("✅ Payment Context initialized with " + paymentStrategies.size() + " strategies");
    }

    public PaymentResult processPayment(Order order, User user, PaymentDetails paymentDetails) {
        try {
            System.out.println("=== PAYMENT CONTEXT: PROCESSING PAYMENT ===");
            System.out.println("Payment Method: " + paymentDetails.getPaymentMethod());
            System.out.println("Order ID: " + order.getId());
            System.out.println("Amount: $" + order.getTotalAmount());

            PaymentStrategy strategy = findStrategy(paymentDetails.getPaymentMethod());

            if (strategy == null) {
                System.out.println("❌ No payment strategy found for: " + paymentDetails.getPaymentMethod());
                return PaymentResult.failure("UNSUPPORTED_METHOD",
                        "Payment method not supported: " + paymentDetails.getPaymentMethod());
            }

            System.out.println("✅ Using payment strategy: " + strategy.getPaymentMethodName());
            PaymentResult result = strategy.processPayment(order, user, paymentDetails);

            System.out.println("💰 Payment Result: " + (result.isSuccess() ? "SUCCESS" : "FAILED"));
            if (result.isSuccess()) {
                System.out.println("📝 Transaction ID: " + result.getTransactionId());
            } else {
                System.out.println("❌ Error: " + result.getMessage());
            }

            return result;

        } catch (Exception e) {
            System.out.println("❌ Payment context error: " + e.getMessage());
            return PaymentResult.failure("SYSTEM_ERROR", "Payment processing error: " + e.getMessage());
        }
    }

    private PaymentStrategy findStrategy(PaymentMethod paymentMethod) {
        return paymentStrategies.stream()
                .filter(strategy -> strategy.supports(paymentMethod))
                .findFirst()
                .orElse(null);
    }

    public List<PaymentMethod> getSupportedPaymentMethods() {
        return paymentStrategies.stream()
                .map(strategy -> {
                    for (PaymentMethod method : PaymentMethod.values()) {
                        if (strategy.supports(method)) {
                            return method;
                        }
                    }
                    return null;
                })
                .filter(method -> method != null)
                .toList();
    }

    public boolean validatePaymentDetails(PaymentDetails paymentDetails) {
        if (paymentDetails == null || paymentDetails.getPaymentMethod() == null) {
            return false;
        }

        PaymentStrategy strategy = findStrategy(paymentDetails.getPaymentMethod());
        return strategy != null;
    }
}