package org.example.musicalinstrumentsystem.repository;

import org.example.musicalinstrumentsystem.entity.Cart;
import org.example.musicalinstrumentsystem.entity.CartItem;
import org.example.musicalinstrumentsystem.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Find cart item by cart and product
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    // Find all cart items by cart
    List<CartItem> findByCart(Cart cart);

    // Delete cart item by cart and product
    void deleteByCartAndProduct(Cart cart, Product product);

    // Count cart items by cart
    Long countByCart(Cart cart);
}