package org.example.musicalinstrumentsystem.payment;

public enum PaymentMethod {
    CREDIT_CARD("Credit Card"),
    PAYPAL("PayPal"),
    STRIPE("Stripe"),
    BANK_TRANSFER("Bank Transfer"),
    CASH_ON_DELIVERY("Cash on Delivery");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}