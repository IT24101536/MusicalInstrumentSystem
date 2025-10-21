package org.example.musicalinstrumentsystem.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    @Column(nullable = false)
    private Double totalAmount = 0.0;

    // Constructors
    public Cart() {}

    public Cart(User user) {
        this.user = user;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<CartItem> getCartItems() { return cartItems; }
    public void setCartItems(List<CartItem> cartItems) { this.cartItems = cartItems; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    // Business methods
    public void addCartItem(CartItem item) {
        cartItems.add(item);
        item.setCart(this);
        calculateTotal();
    }

    public void removeCartItem(CartItem item) {
        cartItems.remove(item);
        item.setCart(null);
        calculateTotal();
    }

    public void calculateTotal() {
        this.totalAmount = cartItems.stream()
                .mapToDouble(CartItem::getSubtotal)
                .sum();
    }

    public void clearCart() {
        cartItems.clear();
        totalAmount = 0.0;
    }

    public boolean isEmpty() {
        return cartItems.isEmpty();
    }

    public int getTotalItems() {
        return cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}