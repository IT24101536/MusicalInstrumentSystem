package org.example.musicalinstrumentsystem.service;

import org.example.musicalinstrumentsystem.entity.*;
import org.example.musicalinstrumentsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get user by ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Get user by email
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Create new user
    public User createUser(User user) {
        return userRepository.save(user);
    }

    // Update user
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    // DELETE USER
    public boolean safeDeleteUser(Long userId) {
        try {
            System.out.println("=== SAFE DELETING USER ===");
            System.out.println("User ID to delete: " + userId);

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                System.out.println("❌ User not found with ID: " + userId);
                return false;
            }

            User user = userOpt.get();
            System.out.println("Deleting user: " + user.getEmail() + " (" + user.getRole() + ")");

            // Handle deletion based on user role
            switch (user.getRole()) {
                case "BUYER":
                    return deleteBuyerUser(user);
                case "SELLER":
                    return deleteSellerUser(user);
                case "STOCK_MANAGER":
                case "ADMIN":
                    return deleteStaffUser(user);
                default:
                    System.out.println("❌ Unknown user role: " + user.getRole());
                    return false;
            }

        } catch (Exception e) {
            System.out.println("❌ Error in safeDeleteUser: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Delete buyer user
    private boolean deleteBuyerUser(User user) {
        try {
            System.out.println("🛒 Deleting buyer user data...");

            deleteUserCartData(user);

            deleteUserOrderData(user);

            userRepository.delete(user);
            System.out.println("✅ Buyer user deleted successfully: " + user.getEmail());
            return true;

        } catch (Exception e) {
            System.out.println("❌ Error deleting buyer user: " + e.getMessage());
            return false;
        }
    }

    // Delete seller user
    private boolean deleteSellerUser(User user) {
        try {
            System.out.println("🏪 Deleting seller user data...");


            transferSellerProducts(user);

            deleteUserCartData(user);


            userRepository.delete(user);
            System.out.println("✅ Seller user deleted successfully: " + user.getEmail());
            return true;

        } catch (Exception e) {
            System.out.println("❌ Error deleting seller user: " + e.getMessage());
            return false;
        }
    }

    // Delete staff user
    private boolean deleteStaffUser(User user) {
        try {
            System.out.println("👔 Deleting staff user data...");

            deleteUserCartData(user);

            // delete the user
            userRepository.delete(user);
            System.out.println("✅ Staff user deleted successfully: " + user.getEmail());
            return true;

        } catch (Exception e) {
            System.out.println("❌ Error deleting staff user: " + e.getMessage());
            return false;
        }
    }

    private void deleteUserCartData(User user) {
        try {
            Optional<Cart> userCart = cartRepository.findByUser(user);
            if (userCart.isPresent()) {
                Cart cart = userCart.get();
                System.out.println("🗑️ Deleting cart items for user: " + user.getEmail());

                // Delete cart items first
                cartItemRepository.deleteAll(cart.getCartItems());

                // Then delete the cart
                cartRepository.delete(cart);
                System.out.println("✅ Cart data deleted for user: " + user.getEmail());
            }
        } catch (Exception e) {
            System.out.println("⚠️ No cart data to delete for user: " + user.getEmail());
        }
    }

    private void deleteUserOrderData(User user) {
        try {
            List<Order> userOrders = orderRepository.findByBuyer(user);
            if (!userOrders.isEmpty()) {
                System.out.println("📦 Deleting " + userOrders.size() + " orders for user: " + user.getEmail());

                for (Order order : userOrders) {
                    // Delete order items first
                    orderItemRepository.deleteAll(order.getOrderItems());
                    // Then delete the order
                    orderRepository.delete(order);
                }
                System.out.println("✅ Order data deleted for user: " + user.getEmail());
            }
        } catch (Exception e) {
            System.out.println("⚠️ No order data to delete for user: " + user.getEmail());
        }
    }

    private void transferSellerProducts(User seller) {
        try {
            List<Product> sellerProducts = productRepository.findBySeller(seller);
            if (!sellerProducts.isEmpty()) {
                System.out.println("🔄 Transferring " + sellerProducts.size() + " products from seller: " + seller.getEmail());

                Optional<User> defaultAdmin = userRepository.findAll().stream()
                        .filter(u -> "ADMIN".equals(u.getRole()) && !u.getId().equals(seller.getId()))
                        .findFirst();

                if (defaultAdmin.isPresent()) {
                    User newOwner = defaultAdmin.get();
                    for (Product product : sellerProducts) {
                        product.setSeller(newOwner);
                        productRepository.save(product);
                    }
                    System.out.println("✅ Products transferred to: " + newOwner.getEmail());
                } else {
                    System.out.println("❌ No admin found, deleting products instead");
                    productRepository.deleteAll(sellerProducts);
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error transferring products: " + e.getMessage());
        }
    }

    // Check if email exists
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    // Get users by role
    public List<User> getUsersByRole(String role) {
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .filter(user -> role.equals(user.getRole()))
                .toList();
    }

    // Count users by role
    public long countUsersByRole(String role) {
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .filter(user -> role.equals(user.getRole()))
                .count();
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}