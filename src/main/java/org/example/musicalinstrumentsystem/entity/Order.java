package org.example.musicalinstrumentsystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private String status; // PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED

    private String shippingAddress;
    private String paymentMethod;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "payment_status")
    private String paymentStatus = "PENDING"; // PENDING, COMPLETED, FAILED, REFUNDED

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "delivery_date")
    private LocalDateTime deliveryDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    // Constructors
    public Order() {
        this.orderDate = LocalDateTime.now();
        this.status = "PENDING";
        this.paymentStatus = "PENDING";
    }

    public Order(User buyer, Double totalAmount, String shippingAddress) {
        this();
        this.buyer = buyer;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getBuyer() { return buyer; }
    public void setBuyer(User buyer) { this.buyer = buyer; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public LocalDateTime getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDateTime deliveryDate) { this.deliveryDate = deliveryDate; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }

    public void addOrderItem(OrderItem item) {
        if (orderItems == null) {
            orderItems = new ArrayList<>();
        }
        orderItems.add(item);
        item.setOrder(this);

        // Recalculate total
        if (this.totalAmount == null) {
            this.totalAmount = 0.0;
        }
        this.totalAmount += item.getSubtotal();
    }

    public boolean isPaid() {
        return "COMPLETED".equals(paymentStatus);
    }

    public boolean canBeCancelled() {
        return "PENDING".equals(status) || "CONFIRMED".equals(status);
    }

    public String getPaymentStatusBadge() {
        if (paymentStatus == null) {
            return "secondary";
        }
        switch (paymentStatus) {
            case "COMPLETED": return "success";
            case "PENDING": return "warning";
            case "FAILED": return "danger";
            case "REFUNDED": return "info";
            default: return "secondary";
        }
    }

    public String getStatusBadge() {
        if (status == null) {
            return "secondary";
        }
        switch (status) {
            case "PENDING": return "warning";
            case "CONFIRMED": return "info";
            case "SHIPPED": return "primary";
            case "DELIVERED": return "success";
            case "CANCELLED": return "danger";
            default: return "secondary";
        }
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", buyer=" + (buyer != null ? buyer.getEmail() : "null") +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", orderDate=" + orderDate +
                '}';
    }
}