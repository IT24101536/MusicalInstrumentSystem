package org.example.musicalinstrumentsystem.repository;

import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Find order items by order
    List<OrderItem> findByOrder(Order order);

    // Find order items by product
    List<OrderItem> findByProductId(Long productId);
}