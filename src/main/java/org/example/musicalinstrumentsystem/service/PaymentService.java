package org.example.musicalinstrumentsystem.service;

import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.Payment;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.payment.*;
import org.example.musicalinstrumentsystem.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(rollbackFor = Exception.class)
public class PaymentService {

    @Autowired
    private PaymentContext paymentContext;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentRecordService paymentRecordService;

    @Transactional(rollbackFor = Exception.class)
    public PaymentResult processOrderPayment(Order order, User user, PaymentDetails paymentDetails) {
        try {
            System.out.println("=== PAYMENT SERVICE: PROCESSING ORDER PAYMENT ===");
            System.out.println("Order ID: " + order.getId());
            System.out.println("User: " + user.getEmail());
            System.out.println("Payment Method: " + paymentDetails.getPaymentMethod());
            System.out.println("Current Order Status: " + order.getStatus());
            System.out.println("Current Payment Status: " + order.getPaymentStatus());

            // Validate order status
            if (!"PENDING".equals(order.getStatus())) {
                System.out.println("❌ Invalid order status: " + order.getStatus());
                return PaymentResult.failure("INVALID_ORDER_STATUS",
                        "Cannot process payment for order with status: " + order.getStatus());
            }

            // Check if already paid
            if ("COMPLETED".equals(order.getPaymentStatus())) {
                System.out.println("❌ Order already paid");
                return PaymentResult.failure("ALREADY_PAID", "Order has already been paid");
            }

            // Process payment using strategy pattern
            System.out.println("🔄 Processing payment with strategy...");
            PaymentResult paymentResult = paymentContext.processPayment(order, user, paymentDetails);

            System.out.println("🔄 Payment Result: " + (paymentResult.isSuccess() ? "SUCCESS" : "FAILED"));
            System.out.println("📝 Transaction ID: " + paymentResult.getTransactionId());
            System.out.println("💬 Message: " + paymentResult.getMessage());

            // Create payment record first
            System.out.println("🔄 Creating payment record...");
            Payment paymentRecord = null;
            try {
                paymentRecord = paymentRecordService.createPayment(order, paymentDetails);
                System.out.println("✅ Payment record created with ID: " + paymentRecord.getId());
            } catch (Exception e) {
                System.out.println("❌ Failed to create payment record: " + e.getMessage());
                e.printStackTrace();
                return PaymentResult.failure("PAYMENT_RECORD_CREATION_FAILED", "Failed to create payment record: " + e.getMessage());
            }
            
            // Update payment record with result
            if (paymentResult.isSuccess()) {
                System.out.println("✅ Payment successful, updating payment record...");
                try {
                    paymentRecordService.updatePaymentStatus(paymentRecord.getId(), paymentResult);
                    System.out.println("✅ Payment record updated successfully");
                } catch (Exception e) {
                    System.out.println("❌ Failed to update payment record: " + e.getMessage());
                    e.printStackTrace();
                    return PaymentResult.failure("PAYMENT_RECORD_UPDATE_FAILED", "Failed to update payment record: " + e.getMessage());
                }
                
                // Use the orderService
                try {
                    Order updatedOrder = orderService.completeOrderPayment(
                            order.getId(),
                            paymentResult.getTransactionId(),
                            paymentDetails.getPaymentMethod().getDisplayName()
                    );

                    if (updatedOrder != null) {
                        System.out.println("✅ Order payment completed successfully");
                        System.out.println("📝 Final Order Status: " + updatedOrder.getStatus());
                        System.out.println("💳 Final Payment Status: " + updatedOrder.getPaymentStatus());
                        System.out.println("💰 Transaction ID: " + updatedOrder.getTransactionId());
                        System.out.println("💾 Payment Record ID: " + paymentRecord.getId());
                    } else {
                        System.out.println("❌ Failed to update order after payment");
                        return PaymentResult.failure("ORDER_UPDATE_FAILED", "Failed to update order status after payment");
                    }
                } catch (Exception e) {
                    System.out.println("❌ Failed to update order: " + e.getMessage());
                    e.printStackTrace();
                    return PaymentResult.failure("ORDER_UPDATE_FAILED", "Failed to update order: " + e.getMessage());
                }
            } else {
                // Update payment record to failed
                System.out.println("❌ Payment failed, updating payment record to FAILED");
                try {
                    paymentRecordService.updatePaymentStatus(paymentRecord.getId(), paymentResult);
                    System.out.println("✅ Payment record updated to FAILED");
                } catch (Exception e) {
                    System.out.println("❌ Failed to update payment record to FAILED: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // Update order status to failed
                System.out.println("❌ Payment failed, updating order status to FAILED");
                try {
                    order.setPaymentStatus("FAILED");
                    orderRepository.save(order);
                    System.out.println("✅ Order status updated to FAILED");
                } catch (Exception e) {
                    System.out.println("❌ Failed to update order status: " + e.getMessage());
                    e.printStackTrace();
                }
                System.out.println("❌ Order payment failed: " + paymentResult.getMessage());
            }

            return paymentResult;

        } catch (Exception e) {
            System.out.println("❌ Payment service error: " + e.getMessage());
            e.printStackTrace();

            // Update order status to failed
            try {
                order.setPaymentStatus("FAILED");
                orderRepository.save(order);
                System.out.println("❌ Order marked as FAILED due to error");
            } catch (Exception saveError) {
                System.out.println("❌ Failed to save order failure status: " + saveError.getMessage());
            }

            return PaymentResult.failure("SYSTEM_ERROR", "Payment processing failed: " + e.getMessage());
        }
    }

    public PaymentResult refundPayment(Order order) {
        try {
            System.out.println("=== PROCESSING PAYMENT REFUND ===");
            System.out.println("Order ID: " + order.getId());
            System.out.println("Transaction ID: " + order.getTransactionId());

            if (order.getTransactionId() == null) {
                return PaymentResult.failure("NO_TRANSACTION", "No transaction ID found for refund");
            }

            if (!"COMPLETED".equals(order.getPaymentStatus())) {
                return PaymentResult.failure("INVALID_STATUS",
                        "Cannot refund payment with status: " + order.getPaymentStatus());
            }

            System.out.println("🔄 Processing refund...");

            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Update order status
            order.setPaymentStatus("REFUNDED");
            orderRepository.save(order);

            String refundId = "RF_" + order.getTransactionId();

            System.out.println("✅ Refund processed successfully");
            System.out.println("📝 Refund ID: " + refundId);

            return PaymentResult.success(refundId, "Payment refunded successfully");

        } catch (Exception e) {
            System.out.println("❌ Refund processing failed: " + e.getMessage());
            return PaymentResult.failure("REFUND_ERROR", "Refund failed: " + e.getMessage());
        }
    }

    public java.util.List<PaymentMethod> getSupportedPaymentMethods() {
        return paymentContext.getSupportedPaymentMethods();
    }

    public boolean validatePaymentDetails(PaymentDetails paymentDetails) {
        return paymentContext.validatePaymentDetails(paymentDetails);
    }


    public String getOrderPaymentStatus(Long orderId) {
        try {
            var orderOpt = orderRepository.findById(orderId);
            return orderOpt.map(Order::getPaymentStatus).orElse("NOT_FOUND");
        } catch (Exception e) {
            System.out.println("❌ Error getting payment status: " + e.getMessage());
            return "ERROR";
        }
    }
}