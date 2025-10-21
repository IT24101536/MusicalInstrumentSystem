package org.example.musicalinstrumentsystem.payment;

import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.User;

public interface PaymentStrategy {
    PaymentResult processPayment(Order order, User user, PaymentDetails paymentDetails);
    boolean supports(PaymentMethod paymentMethod);
    String getPaymentMethodName();
}