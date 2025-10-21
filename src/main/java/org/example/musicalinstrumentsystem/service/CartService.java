package org.example.musicalinstrumentsystem.service;

import org.example.musicalinstrumentsystem.entity.Cart;
import org.example.musicalinstrumentsystem.entity.CartItem;
import org.example.musicalinstrumentsystem.entity.Product;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.repository.CartRepository;
import org.example.musicalinstrumentsystem.repository.CartItemRepository;
import org.example.musicalinstrumentsystem.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    //  CORE CART OPERATIONS

    public Cart getOrCreateCart(User user) {
        try {
            System.out.println("=== GETTING/CREATING CART FOR USER: " + user.getEmail() + " ===");

            Optional<Cart> cartOpt = cartRepository.findByUser(user);
            if (cartOpt.isPresent()) {
                Cart cart = cartOpt.get();
                System.out.println("✅ Found existing cart with ID: " + cart.getId());
                System.out.println("📦 Cart items: " + cart.getCartItems().size());
                return cart;
            } else {
                System.out.println("🆕 Creating new cart for user");
                Cart newCart = new Cart(user);
                Cart savedCart = cartRepository.save(newCart);
                System.out.println("✅ Created new cart with ID: " + savedCart.getId());
                return savedCart;
            }
        } catch (Exception e) {
            System.out.println("❌ Error getting/creating cart: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get or create cart: " + e.getMessage(), e);
        }
    }


    public Cart addToCart(User user, Long productId, Integer quantity) {
        try {
            System.out.println("=== ADDING TO CART ===");
            System.out.println("User: " + user.getEmail());
            System.out.println("Product ID: " + productId);
            System.out.println("Quantity: " + quantity);

            // Validate quantity
            if (quantity == null || quantity <= 0) {
                throw new RuntimeException("Quantity must be greater than 0");
            }

            // Get or create cart
            Cart cart = getOrCreateCart(user);
            System.out.println("Cart ID: " + cart.getId());

            // Find product
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                throw new RuntimeException("Product not found with ID: " + productId);
            }

            Product product = productOpt.get();
            System.out.println("Product found: " + product.getName());
            System.out.println("Product price: " + product.getPrice());
            System.out.println("Product stock: " + product.getStockQuantity());

            // Check stock availability
            if (product.getStockQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName() +
                        ". Available: " + product.getStockQuantity() + ", Requested: " + quantity);
            }

            // Check if product already in cart
            Optional<CartItem> existingItem = cart.getCartItems().stream()
                    .filter(item -> item.getProduct().getId().equals(productId))
                    .findFirst();

            if (existingItem.isPresent()) {
                // Update quantity of existing item
                CartItem item = existingItem.get();
                int newQuantity = item.getQuantity() + quantity;

                // Check if new quantity exceeds stock
                if (product.getStockQuantity() < newQuantity) {
                    throw new RuntimeException("Cannot add " + quantity + " more. Total would be " +
                            newQuantity + " but only " + product.getStockQuantity() + " available.");
                }

                item.setQuantity(newQuantity);
                System.out.println("📝 Updated existing item quantity to: " + newQuantity);
            } else {
                // Create new cart item with ALL required fields
                CartItem newItem = new CartItem();
                newItem.setProduct(product);
                newItem.setQuantity(quantity);
                newItem.setUnitPrice(product.getPrice());
                newItem.setTotalPrice(product.getPrice() * quantity);

                System.out.println("🆕 Creating new cart item:");
                System.out.println("   - Unit Price: " + newItem.getUnitPrice());
                System.out.println("   - Total Price: " + newItem.getTotalPrice());
                System.out.println("   - Quantity: " + newItem.getQuantity());

                cart.addCartItem(newItem);
                System.out.println("✅ Added new item to cart");
            }

            // Calculate total
            cart.calculateTotal();
            System.out.println("💰 Cart total: $" + cart.getTotalAmount());

            // Save cart
            Cart savedCart = cartRepository.save(cart);
            System.out.println("💾 Cart saved successfully");
            System.out.println("📦 Total items in cart: " + savedCart.getCartItems().size());

            return savedCart;

        } catch (Exception e) {
            System.out.println("❌ ERROR adding to cart: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to add item to cart: " + e.getMessage(), e);
        }
    }

    public Cart updateCartItemQuantity(User user, Long productId, Integer quantity) {
        try {
            System.out.println("=== UPDATING CART ITEM QUANTITY ===");
            System.out.println("User: " + user.getEmail());
            System.out.println("Product ID: " + productId);
            System.out.println("New Quantity: " + quantity);

            Cart cart = getOrCreateCart(user);

            // Find product to check stock
            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                throw new RuntimeException("Product not found with ID: " + productId);
            }

            Product product = productOpt.get();

            // Validate quantity
            if (quantity <= 0) {
                // Remove item if quantity is 0 or negative
                return removeFromCart(user, productId);
            }

            // Check stock availability
            if (product.getStockQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName() +
                        ". Available: " + product.getStockQuantity() + ", Requested: " + quantity);
            }

            // Update quantity
            cart.getCartItems().stream()
                    .filter(item -> item.getProduct().getId().equals(productId))
                    .findFirst()
                    .ifPresent(item -> {
                        item.setQuantity(quantity);
                        System.out.println("✅ Updated quantity for " + product.getName() + " to " + quantity);
                    });

            cart.calculateTotal();
            Cart savedCart = cartRepository.save(cart);
            System.out.println("💰 Updated cart total: $" + savedCart.getTotalAmount());

            return savedCart;
        } catch (Exception e) {
            System.out.println("❌ Error updating cart quantity: " + e.getMessage());
            throw new RuntimeException("Failed to update cart quantity: " + e.getMessage(), e);
        }
    }

    public Cart removeFromCart(User user, Long productId) {
        try {
            System.out.println("=== REMOVING FROM CART ===");
            System.out.println("User: " + user.getEmail());
            System.out.println("Product ID: " + productId);

            Cart cart = getOrCreateCart(user);

            boolean removed = cart.getCartItems().removeIf(item ->
                    item.getProduct().getId().equals(productId)
            );

            if (removed) {
                System.out.println("✅ Removed product from cart");
                cart.calculateTotal();
                Cart savedCart = cartRepository.save(cart);
                System.out.println("💰 Updated cart total: $" + savedCart.getTotalAmount());
                return savedCart;
            } else {
                System.out.println("⚠️ Product not found in cart");
                return cart;
            }
        } catch (Exception e) {
            System.out.println("❌ Error removing from cart: " + e.getMessage());
            throw new RuntimeException("Failed to remove item from cart: " + e.getMessage(), e);
        }
    }

    public Cart clearCart(User user) {
        try {
            System.out.println("=== CLEARING CART ===");
            System.out.println("User: " + user.getEmail());

            Cart cart = getOrCreateCart(user);
            cart.clearCart();

            Cart savedCart = cartRepository.save(cart);
            System.out.println("✅ Cart cleared successfully");

            return savedCart;
        } catch (Exception e) {
            System.out.println("❌ Error clearing cart: " + e.getMessage());
            throw new RuntimeException("Failed to clear cart: " + e.getMessage(), e);
        }
    }

    public Integer getCartItemCount(User user) {
        try {
            Cart cart = getOrCreateCart(user);
            int count = cart.getTotalItems();
            System.out.println("🛒 Cart item count for " + user.getEmail() + ": " + count);
            return count;
        } catch (Exception e) {
            System.out.println("❌ Error getting cart count: " + e.getMessage());
            return 0;
        }
    }


    public Double getCartTotal(User user) {
        try {
            Cart cart = getOrCreateCart(user);
            double total = cart.getTotalAmount() != null ? cart.getTotalAmount() : 0.0;
            System.out.println("💰 Cart total for " + user.getEmail() + ": $" + total);
            return total;
        } catch (Exception e) {
            System.out.println("❌ Error getting cart total: " + e.getMessage());
            return 0.0;
        }
    }


    public Optional<CartItem> getCartItemByProduct(User user, Long productId) {
        try {
            Cart cart = getOrCreateCart(user);
            Optional<CartItem> item = cart.getCartItems().stream()
                    .filter(cartItem -> cartItem.getProduct().getId().equals(productId))
                    .findFirst();

            System.out.println("🔍 Cart item lookup for product " + productId + ": " +
                    (item.isPresent() ? "FOUND" : "NOT FOUND"));

            return item;
        } catch (Exception e) {
            System.out.println("❌ Error getting cart item: " + e.getMessage());
            return Optional.empty();
        }
    }


    public boolean isProductInCart(User user, Long productId) {
        boolean inCart = getCartItemByProduct(user, productId).isPresent();
        System.out.println("🔍 Product " + productId + " in cart: " + inCart);
        return inCart;
    }


    public Map<String, Object> getCartSummary(User user) {
        try {
            Cart cart = getOrCreateCart(user);
            Map<String, Object> summary = new HashMap<>();
            summary.put("itemCount", cart.getTotalItems());
            summary.put("totalAmount", cart.getTotalAmount() != null ? cart.getTotalAmount() : 0.0);
            summary.put("cartItems", cart.getCartItems().size());
            summary.put("isEmpty", cart.isEmpty());

            System.out.println("📊 Cart summary for " + user.getEmail() + ": " + summary);

            return summary;
        } catch (Exception e) {
            System.out.println("❌ Error getting cart summary: " + e.getMessage());
            return Map.of(
                    "itemCount", 0,
                    "totalAmount", 0.0,
                    "cartItems", 0,
                    "isEmpty", true
            );
        }
    }

    //VALIDATION METHODS

    public boolean validateCartForCheckout(User user) {
        try {
            Cart cart = getOrCreateCart(user);

            if (cart.isEmpty()) {
                System.out.println("❌ Cart is empty");
                return false;
            }

            // Check stock for all items
            for (CartItem item : cart.getCartItems()) {
                Product product = item.getProduct();
                if (product.getStockQuantity() < item.getQuantity()) {
                    System.out.println("❌ Insufficient stock for: " + product.getName() +
                            " (Available: " + product.getStockQuantity() + ", Needed: " + item.getQuantity() + ")");
                    return false;
                }
            }

            System.out.println("✅ Cart validated successfully for checkout");
            return true;
        } catch (Exception e) {
            System.out.println("❌ Error validating cart: " + e.getMessage());
            return false;
        }
    }


    public Map<String, String> getCartValidationErrors(User user) {
        Map<String, String> errors = new HashMap<>();
        try {
            Cart cart = getOrCreateCart(user);

            if (cart.isEmpty()) {
                errors.put("empty", "Your cart is empty");
            }

            // Check stock for each item
            for (CartItem item : cart.getCartItems()) {
                Product product = item.getProduct();
                if (product.getStockQuantity() < item.getQuantity()) {
                    errors.put(product.getId().toString(),
                            "Insufficient stock for " + product.getName() +
                                    " (Available: " + product.getStockQuantity() + ", In cart: " + item.getQuantity() + ")");
                }
            }

            return errors;
        } catch (Exception e) {
            errors.put("system", "System error: " + e.getMessage());
            return errors;
        }
    }

    // UTILITY METHODS

    public Cart refreshCart(User user) {
        try {
            System.out.println("=== REFRESHING CART ===");
            Cart cart = getOrCreateCart(user);

            // Update prices and recalculate
            for (CartItem item : cart.getCartItems()) {
                // Refresh product data
                Optional<Product> currentProduct = productRepository.findById(item.getProduct().getId());
                if (currentProduct.isPresent()) {
                    Product product = currentProduct.get();
                    item.setUnitPrice(product.getPrice());
                    item.setTotalPrice(product.getPrice() * item.getQuantity());
                }
            }

            cart.calculateTotal();
            Cart savedCart = cartRepository.save(cart);

            System.out.println("✅ Cart refreshed successfully");
            System.out.println("💰 New total: $" + savedCart.getTotalAmount());

            return savedCart;
        } catch (Exception e) {
            System.out.println("❌ Error refreshing cart: " + e.getMessage());
            throw new RuntimeException("Failed to refresh cart: " + e.getMessage(), e);
        }
    }

    public Cart mergeCarts(User user, Cart guestCart) {
        try {
            System.out.println("=== MERGING CARTS ===");
            Cart userCart = getOrCreateCart(user);

            for (CartItem guestItem : guestCart.getCartItems()) {
                addToCart(user, guestItem.getProduct().getId(), guestItem.getQuantity());
            }

            System.out.println("✅ Carts merged successfully");
            return userCart;
        } catch (Exception e) {
            System.out.println("❌ Error merging carts: " + e.getMessage());
            throw new RuntimeException("Failed to merge carts: " + e.getMessage(), e);
        }
    }


    public void printCartDetails(User user) {
        try {
            Cart cart = getOrCreateCart(user);
            System.out.println("=== CART DETAILS ===");
            System.out.println("Cart ID: " + cart.getId());
            System.out.println("User: " + user.getEmail());
            System.out.println("Total Items: " + cart.getTotalItems());
            System.out.println("Total Amount: $" + cart.getTotalAmount());
            System.out.println("Items in cart:");

            for (CartItem item : cart.getCartItems()) {
                System.out.println("  - " + item.getProduct().getName() +
                        " x" + item.getQuantity() + " = $" + item.getSubtotal());
            }
            System.out.println("====================");
        } catch (Exception e) {
            System.out.println("❌ Error printing cart details: " + e.getMessage());
        }
    }
}