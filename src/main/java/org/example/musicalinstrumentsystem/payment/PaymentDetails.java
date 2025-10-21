package org.example.musicalinstrumentsystem.payment;

import java.util.Map;

public class PaymentDetails {
    private PaymentMethod paymentMethod;
    private Map<String, String> paymentData;
    private String transactionId;
    private String description;

    // Constructors
    public PaymentDetails() {}

    public PaymentDetails(PaymentMethod paymentMethod, Map<String, String> paymentData) {
        this.paymentMethod = paymentMethod;
        this.paymentData = paymentData;
    }

    // Getters and Setters
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public Map<String, String> getPaymentData() { return paymentData; }
    public void setPaymentData(Map<String, String> paymentData) { this.paymentData = paymentData; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPaymentData(String key) {
        return paymentData != null ? paymentData.get(key) : null;
    }

    public void addPaymentData(String key, String value) {
        if (this.paymentData == null) {
            this.paymentData = new java.util.HashMap<>();
        }
        this.paymentData.put(key, value);
    }
}