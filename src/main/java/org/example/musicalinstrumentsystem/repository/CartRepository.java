package org.example.musicalinstrumentsystem.repository;

import org.example.musicalinstrumentsystem.entity.Cart;
import org.example.musicalinstrumentsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Find cart by user
    Optional<Cart> findByUser(User user);

    // Check if cart exists for user
    boolean existsByUser(User user);
}