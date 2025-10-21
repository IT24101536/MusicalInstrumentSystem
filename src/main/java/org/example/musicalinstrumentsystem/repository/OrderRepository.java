package org.example.musicalinstrumentsystem.repository;

import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Find orders by buyer
    List<Order> findByBuyer(User buyer);

    // Find orders by status
    List<Order> findByStatus(String status);

    // Find orders by buyer and status
    List<Order> findByBuyerAndStatus(User buyer, String status);

    // Count orders by status
    Long countByStatus(String status);

    // Get total sales amount
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'DELIVERED'")
    Double getTotalSales();

    // Find recent orders
    @Query("SELECT o FROM Order o ORDER BY o.orderDate DESC")
    List<Order> findRecentOrders();
}