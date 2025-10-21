package org.example.musicalinstrumentsystem.repository;

import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payment by order
    Optional<Payment> findByOrder(Order order);

    // Find payment by transaction ID
    Optional<Payment> findByTransactionId(String transactionId);

    // Find payments by status
    List<Payment> findByStatus(String status);

    // Find payments by payment method
    List<Payment> findByPaymentMethod(String paymentMethod);

    // Check if payment exists for order
    boolean existsByOrder(Order order);

    // Get total revenue (sum of completed payments)
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED'")
    Double getTotalRevenue();

    // Get recent payments
    @Query("SELECT p FROM Payment p ORDER BY p.paymentDate DESC")
    List<Payment> findRecentPayments();

    // Find payments by date range
    @Query("SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    List<Payment> findByPaymentDateBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                           @Param("endDate") java.time.LocalDateTime endDate);

    // Count payments by status
    Long countByStatus(String status);
}