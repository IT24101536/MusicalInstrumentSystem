package org.example.musicalinstrumentsystem.payment;

import java.time.LocalDateTime;

public class PaymentResult {
    private boolean success;
    private String transactionId;
    private String message;
    private LocalDateTime timestamp;
    private String errorCode;
    private Double amount;

    // Constructors
    public PaymentResult() {
        this.timestamp = LocalDateTime.now();
    }

    public PaymentResult(boolean success, String transactionId, String message) {
        this();
        this.success = success;
        this.transactionId = transactionId;
        this.message = message;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    // Static factory methods
    public static PaymentResult success(String transactionId, String message) {
        return new PaymentResult(true, transactionId, message);
    }

    public static PaymentResult failure(String message) {
        return new PaymentResult(false, null, message);
    }

    public static PaymentResult failure(String errorCode, String message) {
        PaymentResult result = new PaymentResult(false, null, message);
        result.setErrorCode(errorCode);
        return result;
    }

    @Override
    public String toString() {
        return "PaymentResult{" +
                "success=" + success +
                ", transactionId='" + transactionId + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", errorCode='" + errorCode + '\'' +
                ", amount=" + amount +
                '}';
    }
}