package org.example.musicalinstrumentsystem.controller;

import org.example.musicalinstrumentsystem.entity.Cart;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.service.CartService;
import org.example.musicalinstrumentsystem.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/buyer/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private SessionService sessionService;

    // View cart
    @GetMapping
    public String viewCart(Model model) {
        System.out.println("=== VIEWING CART ===");

        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            User currentUser = sessionService.getCurrentUser();
            Cart cart = cartService.getOrCreateCart(currentUser);

            System.out.println("Cart items: " + cart.getCartItems().size());
            System.out.println("Cart total: $" + cart.getTotalAmount());

            model.addAttribute("user", currentUser);
            model.addAttribute("cart", cart);
            model.addAttribute("cartItems", cart.getCartItems());
            model.addAttribute("cartTotal", cart.getTotalAmount());

            return "buyer/cart";
        } catch (Exception e) {
            System.out.println(" Error viewing cart: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/buyer/products?error=cart_error";
        }
    }

    // Add to cart
    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity) {
        System.out.println("=== ADD TO CART REQUEST ===");
        System.out.println("Product ID: " + productId);
        System.out.println("Quantity: " + quantity);

        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            User currentUser = sessionService.getCurrentUser();
            System.out.println("User: " + currentUser.getEmail());

            cartService.addToCart(currentUser, productId, quantity);
            System.out.println(" Successfully added to cart");

            return "redirect:/buyer/cart?success=added_to_cart";

        } catch (Exception e) {
            System.out.println(" Error adding to cart: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/buyer/products?error=cart_error&message=" + e.getMessage();
        }
    }

    // Update cart item quantity
    @PostMapping("/update")
    public String updateCartItem(@RequestParam Long productId,
                                 @RequestParam Integer quantity) {
        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            User currentUser = sessionService.getCurrentUser();
            cartService.updateCartItemQuantity(currentUser, productId, quantity);
            return "redirect:/buyer/cart?success=cart_updated";
        } catch (Exception e) {
            System.out.println(" Error updating cart: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/buyer/cart?error=update_failed";
        }
    }

    // Remove from cart
    @GetMapping("/remove/{productId}")
    public String removeFromCart(@PathVariable Long productId) {
        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            User currentUser = sessionService.getCurrentUser();
            cartService.removeFromCart(currentUser, productId);
            return "redirect:/buyer/cart?success=item_removed";
        } catch (Exception e) {
            System.out.println(" Error removing from cart: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/buyer/cart?error=remove_failed";
        }
    }

    // Clear cart
    @GetMapping("/clear")
    public String clearCart() {
        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            User currentUser = sessionService.getCurrentUser();
            cartService.clearCart(currentUser);
            return "redirect:/buyer/cart?success=cart_cleared";
        } catch (Exception e) {
            System.out.println(" Error clearing cart: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/buyer/cart?error=clear_failed";
        }
    }

    // Get cart count (for AJAX requests)
    @GetMapping("/count")
    @ResponseBody
    public Integer getCartCount() {
        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return 0;
        }

        try {
            User currentUser = sessionService.getCurrentUser();
            return cartService.getCartItemCount(currentUser);
        } catch (Exception e) {
            System.out.println(" Error getting cart count: " + e.getMessage());
            return 0;
        }
    }
}