package org.example.musicalinstrumentsystem.controller;

import org.example.musicalinstrumentsystem.entity.Cart;
import org.example.musicalinstrumentsystem.entity.CartItem;
import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.service.CartService;
import org.example.musicalinstrumentsystem.service.OrderService;
import org.example.musicalinstrumentsystem.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/buyer/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private SessionService sessionService;

    // View order history
    @GetMapping
    public String viewOrders(Model model) {
        System.out.println("=== VIEWING ORDER HISTORY ===");

        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();
        List<Order> orders = orderService.getOrdersByBuyer(currentUser);

        model.addAttribute("user", currentUser);
        model.addAttribute("orders", orders != null ? orders : List.of());

        return "buyer/orders";
    }

    // View order details
    @GetMapping("/{orderId}")
    public String viewOrderDetails(@PathVariable Long orderId, Model model) {
        System.out.println("=== VIEWING ORDER DETAILS ===");
        System.out.println("Order ID: " + orderId);

        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();
        var orderOpt = orderService.getOrderById(orderId);

        if (orderOpt.isEmpty()) {
            return "redirect:/buyer/orders?error=order_not_found";
        }

        Order order = orderOpt.get();

        // Check if the order belongs to the current user
        if (!order.getBuyer().getId().equals(currentUser.getId())) {
            return "redirect:/buyer/orders?error=access_denied";
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("order", order);

        return "buyer/order-details";
    }

    // FIXED: Create order and redirect to payment
    @PostMapping("/create")
    public String createOrder(@RequestParam String shippingAddress) {
        System.out.println("=== CREATING ORDER ===");
        System.out.println("Shipping Address: " + shippingAddress);

        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();
        Cart cart = cartService.getOrCreateCart(currentUser);

        System.out.println("🛒 Cart items count: " + cart.getCartItems().size());
        System.out.println("💰 Cart total: $" + cart.getTotalAmount());

        if (cart.isEmpty()) {
            System.out.println(" Cart is empty");
            return "redirect:/buyer/cart?error=empty_cart";
        }

        try {
            // Validate cart before checkout
            if (!cartService.validateCartForCheckout(currentUser)) {
                System.out.println(" Cart validation failed");
                Map<String, String> errors = cartService.getCartValidationErrors(currentUser);
                System.out.println("Validation errors: " + errors);
                return "redirect:/buyer/cart?error=checkout_validation_failed";
            }

            // Convert cart items to product quantities map
            Map<Long, Integer> productQuantities = new HashMap<>();
            for (CartItem item : cart.getCartItems()) {
                if (item.getProduct() != null) {
                    productQuantities.put(item.getProduct().getId(), item.getQuantity());
                    System.out.println(" Product: " + item.getProduct().getName() + " x " + item.getQuantity());
                }
            }

            // Create order
            Order order = orderService.createOrder(currentUser, productQuantities, shippingAddress);

            if (order == null) {
                System.out.println(" Order creation returned null");
                return "redirect:/buyer/cart?error=order_creation_failed";
            }

            // Clear cart after successful order creation
            cartService.clearCart(currentUser);

            System.out.println(" Order created successfully: " + order.getId());
            System.out.println(" Order total: $" + order.getTotalAmount());


            return "redirect:/payment/order/" + order.getId();

        } catch (Exception e) {
            System.out.println(" Error creating order: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/buyer/cart?error=order_failed&message=" + e.getMessage();
        }
    }

    // Cancel order
    @GetMapping("/cancel/{orderId}")
    public String cancelOrder(@PathVariable Long orderId) {
        System.out.println("=== CANCELLING ORDER ===");
        System.out.println("Order ID: " + orderId);

        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();
        var orderOpt = orderService.getOrderById(orderId);

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();


            if (order.getBuyer().getId().equals(currentUser.getId()) &&
                    order.canBeCancelled()) {

                boolean cancelled = orderService.cancelOrder(orderId);
                if (cancelled) {
                    return "redirect:/buyer/orders?success=order_cancelled";
                }
            }
        }

        return "redirect:/buyer/orders?error=cannot_cancel";
    }
}