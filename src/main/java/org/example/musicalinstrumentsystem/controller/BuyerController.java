package org.example.musicalinstrumentsystem.controller;

import org.example.musicalinstrumentsystem.entity.Product;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.service.ProductService;
import org.example.musicalinstrumentsystem.service.SessionService;
import org.example.musicalinstrumentsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/buyer")
public class BuyerController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    //  BUYER DASHBOARD
    @GetMapping("/dashboard")
    public String buyerDashboard(Model model) {
        System.out.println("=== ACCESSING BUYER DASHBOARD ===");

        if (!sessionService.isLoggedIn()) {
            System.out.println(" Access denied: Not logged in");
            return "redirect:/login?error=access_denied";
        }

        if (!"BUYER".equals(sessionService.getUserRole())) {
            System.out.println(" Access denied: User role is " + sessionService.getUserRole() + ", expected BUYER");
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();
        System.out.println(" Access granted for buyer: " + currentUser.getEmail());

        try {
            // Get products for buyer dashboard
            List<Product> availableProducts = productService.getAvailableProducts();
            List<Product> recentProducts = productService.getRecentProducts(7); // Last 7 days

            // Ensure lists are never null
            if (availableProducts == null) {
                availableProducts = new ArrayList<>();
            }
            if (recentProducts == null) {
                recentProducts = new ArrayList<>();
            }

            // Get categories for filtering
            List<String> categories = List.of(
                    "Guitars", "Pianos & Keyboards", "Drums & Percussion",
                    "String Instruments", "Wind Instruments", "Brass Instruments",
                    "Accessories", "Amplifiers", "Recording Equipment"
            );

            System.out.println(" Dashboard Data:");
            System.out.println("   - Available Products: " + availableProducts.size());
            System.out.println("   - Recent Products: " + recentProducts.size());
            System.out.println("   - Categories: " + categories.size());

            model.addAttribute("user", currentUser);
            model.addAttribute("availableProducts", availableProducts);
            model.addAttribute("recentProducts", recentProducts);
            model.addAttribute("categories", categories);
            model.addAttribute("totalProducts", availableProducts.size());

            return "buyer/dashboard";

        } catch (Exception e) {
            System.out.println(" ERROR in buyerDashboard: " + e.getMessage());
            e.printStackTrace();

            // Return safe default values on error
            model.addAttribute("user", currentUser);
            model.addAttribute("availableProducts", new ArrayList<Product>());
            model.addAttribute("recentProducts", new ArrayList<Product>());
            model.addAttribute("categories", List.of("Guitars", "Pianos", "Drums"));
            model.addAttribute("totalProducts", 0);
            model.addAttribute("error", "Failed to load dashboard data");

            return "buyer/dashboard";
        }
    }

    //  BROWSE PRODUCTS
    @GetMapping("/products")
    public String browseProducts(Model model,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(required = false) String search) {
        System.out.println("=== BROWSE PRODUCTS ===");

        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        List<Product> products;

        if (category != null && !category.isEmpty()) {
            // Filter by category
            products = productService.getProductsByCategory(category);
            model.addAttribute("selectedCategory", category);
        } else if (search != null && !search.isEmpty()) {
            // Search products
            products = productService.searchProducts(search);
            model.addAttribute("searchQuery", search);
        } else {
            // All available products
            products = productService.getAvailableProducts();
        }

        // Ensure products list is never null
        if (products == null) {
            products = new ArrayList<>();
        }

        List<String> categories = List.of(
                "Guitars", "Pianos & Keyboards", "Drums & Percussion",
                "String Instruments", "Wind Instruments", "Brass Instruments",
                "Accessories", "Amplifiers", "Recording Equipment", "Other"
        );

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("user", sessionService.getCurrentUser());

        return "buyer/products";
    }

    //  VIEW PRODUCT DETAILS
    @GetMapping("/products/{id}")
    public String viewProduct(@PathVariable Long id, Model model) {
        System.out.println("=== VIEW PRODUCT DETAILS ===");
        System.out.println("Product ID: " + id);

        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        var productOpt = productService.getProductById(id);
        if (productOpt.isEmpty()) {
            return "redirect:/buyer/products?error=product_not_found";
        }

        Product product = productOpt.get();

        // Get related products (same category)
        List<Product> relatedProducts = productService.getProductsByCategory(product.getCategory());
        if (relatedProducts == null) {
            relatedProducts = new ArrayList<>();
        }
        relatedProducts = relatedProducts.stream()
                .filter(p -> !p.getId().equals(product.getId()))
                .limit(4)
                .toList();

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", relatedProducts);
        model.addAttribute("user", sessionService.getCurrentUser());

        return "buyer/product-details";
    }

    //  BUYER PROFILE MANAGEMENT
    @GetMapping("/profile")
    public String viewProfile(Model model) {
        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login";
        }

        model.addAttribute("user", sessionService.getCurrentUser());
        return "buyer/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String name,
                                @RequestParam String phone,
                                @RequestParam String address,
                                Model model) {
        System.out.println("=== UPDATING BUYER PROFILE ===");

        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login";
        }

        User currentUser = sessionService.getCurrentUser();
        currentUser.setName(name);
        currentUser.setPhone(phone);
        currentUser.setAddress(address);

        userService.updateUser(currentUser);

        // Update session
        sessionService.login(currentUser);

        model.addAttribute("user", currentUser);
        model.addAttribute("success", "Profile updated successfully!");

        return "buyer/profile";
    }

    @GetMapping("/wishlist")
    public String viewWishlist(Model model) {
        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login";
        }

        model.addAttribute("user", sessionService.getCurrentUser());
        // In a real application, you would fetch wishlist items from a database
        model.addAttribute("wishlistItems", List.of()); // Empty for now

        return "buyer/wishlist";
    }

    //  SEARCH PRODUCTS
    @GetMapping("/search")
    public String searchProducts(@RequestParam String query, Model model) {
        System.out.println("=== SEARCHING PRODUCTS ===");
        System.out.println("Query: " + query);

        if (!sessionService.isLoggedIn() || !"BUYER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        List<Product> searchResults = productService.searchProducts(query);
        if (searchResults == null) {
            searchResults = new ArrayList<>();
        }

        model.addAttribute("products", searchResults);
        model.addAttribute("searchQuery", query);
        model.addAttribute("user", sessionService.getCurrentUser());
        model.addAttribute("resultCount", searchResults.size());

        return "buyer/search-results";
    }

    //  TEST DASHBOARD
    @GetMapping("/test-dashboard")
    public String testBuyerDashboard(Model model) {
        System.out.println("=== TESTING BUYER DASHBOARD ===");

        // Create a mock user for testing
        User testUser = new User("test@buyer.com", "password", "Test Buyer", "BUYER");
        testUser.setId(1L);

        // Mock some products for testing
        List<Product> testProducts = new ArrayList<>();

        // Add a test product
        Product testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Guitar");
        testProduct.setCategory("Guitars");
        testProduct.setBrand("Fender");
        testProduct.setPrice(999.99);
        testProduct.setStockQuantity(5);
        testProduct.setDescription("A beautiful test guitar for demonstration");
        testProduct.setSeller(testUser);

        testProducts.add(testProduct);

        List<String> categories = List.of(
                "Guitars", "Pianos & Keyboards", "Drums & Percussion",
                "String Instruments", "Wind Instruments", "Brass Instruments",
                "Accessories", "Amplifiers", "Recording Equipment"
        );

        model.addAttribute("user", testUser);
        model.addAttribute("availableProducts", testProducts);
        model.addAttribute("recentProducts", testProducts);
        model.addAttribute("categories", categories);
        model.addAttribute("totalProducts", testProducts.size());

        return "buyer/dashboard";
    }
}