package org.example.musicalinstrumentsystem.service;

import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.OrderItem;
import org.example.musicalinstrumentsystem.entity.Product;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.repository.OrderRepository;
import org.example.musicalinstrumentsystem.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    // Create new order
    public Order createOrder(User buyer, Map<Long, Integer> productQuantities, String shippingAddress) {
        Order order = new Order();
        order.setBuyer(buyer);
        order.setShippingAddress(shippingAddress);
        order.setStatus("PENDING");

        double totalAmount = 0.0;

        for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();

                // Check stock availability
                if (product.getStockQuantity() < quantity) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName());
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setQuantity(quantity);
                orderItem.setUnitPrice(product.getPrice());

                order.addOrderItem(orderItem);
                totalAmount += orderItem.getSubtotal();

                // Update product stock
                product.setStockQuantity(product.getStockQuantity() - quantity);
                productRepository.save(product);
            }
        }

        order.setTotalAmount(totalAmount);
        return orderRepository.save(order);
    }

    // Get order by ID
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    // Get orders by buyer
    public List<Order> getOrdersByBuyer(User buyer) {
        return orderRepository.findByBuyer(buyer);
    }

    // Get all orders
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // Update order status
    public Order updateOrderStatus(Long orderId, String status) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(status);
            return orderRepository.save(order);
        }
        return null;
    }

    // Complete order payment
    @Transactional(rollbackFor = Exception.class)
    public Order completeOrderPayment(Long orderId, String transactionId, String paymentMethod) {
        System.out.println("=== COMPLETING ORDER PAYMENT ===");
        System.out.println("Order ID: " + orderId);
        System.out.println("Transaction ID: " + transactionId);
        System.out.println("Payment Method: " + paymentMethod);

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setPaymentStatus("COMPLETED");
            order.setTransactionId(transactionId);
            order.setPaymentMethod(paymentMethod);
            order.setPaymentDate(LocalDateTime.now());
            order.setStatus("CONFIRMED"); // Move to confirmed status after payment

            Order savedOrder = orderRepository.save(order);

            System.out.println(" Order payment completed successfully");
            System.out.println(" Order Status: " + savedOrder.getStatus());
            System.out.println(" Payment Status: " + savedOrder.getPaymentStatus());
            System.out.println(" Transaction ID: " + savedOrder.getTransactionId());

            return savedOrder;
        }
        System.out.println(" Order not found for payment completion: " + orderId);
        return null;
    }

    // Save order
    public Order saveOrder(Order order) {
        try {
            System.out.println("=== SAVING ORDER ===");
            System.out.println("Order ID: " + order.getId());
            System.out.println("Status: " + order.getStatus());
            System.out.println("Payment Status: " + order.getPaymentStatus());
            
            Order savedOrder = orderRepository.save(order);
            System.out.println(" Order saved successfully");
            return savedOrder;
        } catch (Exception e) {
            System.out.println(" Error saving order: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save order", e);
        }
    }

    // Update order
    @Transactional
    public Order updateOrder(Order order) {
        try {
            System.out.println("=== UPDATING ORDER ===");
            System.out.println("Order ID: " + order.getId());
            System.out.println("New Status: " + order.getStatus());
            System.out.println("Payment Status: " + order.getPaymentStatus());
            System.out.println("Delivery Date: " + order.getDeliveryDate());
            
            // Force flush to ensure immediate database update
            Order updatedOrder = orderRepository.save(order);
            
            System.out.println(" Order updated successfully in database");
            System.out.println("Updated Order - ID: " + updatedOrder.getId() + ", Status: " + updatedOrder.getStatus());
            
            return updatedOrder;
        } catch (Exception e) {
            System.out.println(" Error updating order: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update order", e);
        }
    }

    // Get total order count
    public long getTotalOrderCount() {
        try {
            return orderRepository.count();
        } catch (Exception e) {
            System.out.println(" Error getting order count: " + e.getMessage());
            return 0;
        }
    }

    // Cancel order and restore stock
    public boolean cancelOrder(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();

            // Restore product stock
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }

            order.setStatus("CANCELLED");
            orderRepository.save(order);
            return true;
        }
        return false;
    }

    // Get order statistics
    public Map<String, Object> getOrderStatistics() {
        Long pendingOrders = orderRepository.countByStatus("PENDING");
        Long deliveredOrders = orderRepository.countByStatus("DELIVERED");
        Double totalSales = orderRepository.getTotalSales();

        return Map.of(
                "pendingOrders", pendingOrders != null ? pendingOrders : 0,
                "deliveredOrders", deliveredOrders != null ? deliveredOrders : 0,
                "totalSales", totalSales != null ? totalSales : 0.0
        );
    }

    // Delete order
    @Transactional
    public boolean deleteOrder(Long orderId) {
        try {
            System.out.println("=== DELETING ORDER: " + orderId + " ===");
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            
            if (orderOpt.isEmpty()) {
                System.out.println(" Order not found with ID: " + orderId);
                return false;
            }
            
            Order order = orderOpt.get();

            if (!"DELIVERED".equals(order.getStatus()) && !"CANCELLED".equals(order.getStatus())) {
                System.out.println(" Restoring stock for deleted order");
                for (OrderItem item : order.getOrderItems()) {
                    Product product = item.getProduct();
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                    productRepository.save(product);
                }
            }
            
            // Delete the order
            orderRepository.deleteById(orderId);
            System.out.println(" Order deleted successfully");
            return true;
            
        } catch (Exception e) {
            System.out.println(" Error deleting order: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}