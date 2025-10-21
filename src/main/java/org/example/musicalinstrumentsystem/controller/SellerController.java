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
@RequestMapping("/seller")
public class SellerController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    //  SELLER DASHBOARD
    @GetMapping("/dashboard")
    public String sellerDashboard(Model model) {
        System.out.println("=== ACCESSING SELLER DASHBOARD ===");

        if (!sessionService.isLoggedIn()) {
            System.out.println("❌ Access denied: Not logged in");
            return "redirect:/login?error=access_denied";
        }

        if (!"SELLER".equals(sessionService.getUserRole())) {
            System.out.println("❌ Access denied: User role is " + sessionService.getUserRole() + ", expected SELLER");
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();
        System.out.println("✅ Access granted for seller: " + currentUser.getEmail());

        try {
            // Add product statistics for seller dashboard
            List<Product> sellerProducts = productService.getProductsBySeller(currentUser);

            // Ensure list is never null
            if (sellerProducts == null) {
                sellerProducts = new ArrayList<>();
            }

            long productCount = sellerProducts.size();
            long lowStockCount = sellerProducts.stream().filter(Product::isLowStock).count();
            long outOfStockCount = sellerProducts.stream().filter(p -> p.getStockQuantity() == 0).count();
            double totalStockValue = productService.getTotalStockValueBySeller(currentUser);

            System.out.println("📊 Seller Dashboard Data:");
            System.out.println("   - Total Products: " + productCount);
            System.out.println("   - Low Stock: " + lowStockCount);
            System.out.println("   - Out of Stock: " + outOfStockCount);
            System.out.println("   - Total Value: $" + totalStockValue);

            model.addAttribute("user", currentUser);
            model.addAttribute("productCount", productCount);
            model.addAttribute("lowStockCount", lowStockCount);
            model.addAttribute("outOfStockCount", outOfStockCount);
            model.addAttribute("totalStockValue", totalStockValue);
            model.addAttribute("sellerProducts", sellerProducts);

            return "seller/dashboard";

        } catch (Exception e) {
            System.out.println("❌ ERROR in sellerDashboard: " + e.getMessage());
            e.printStackTrace();

            // Return safe default values on error
            model.addAttribute("user", currentUser);
            model.addAttribute("productCount", 0);
            model.addAttribute("lowStockCount", 0);
            model.addAttribute("outOfStockCount", 0);
            model.addAttribute("totalStockValue", 0.0);
            model.addAttribute("sellerProducts", new ArrayList<Product>());
            model.addAttribute("error", "Failed to load dashboard data");

            return "seller/dashboard";
        }
    }

    //  SELLER PROFILE
    @GetMapping("/profile")
    public String sellerProfile(Model model) {
        if (!sessionService.isLoggedIn() || !"SELLER".equals(sessionService.getUserRole())) {
            return "redirect:/login";
        }
        model.addAttribute("user", sessionService.getCurrentUser());
        return "seller/profile";
    }

    @PostMapping("/profile/update")
    public String updateSellerProfile(@RequestParam String name,
                                      @RequestParam String phone,
                                      @RequestParam String address,
                                      Model model) {
        System.out.println("=== UPDATING SELLER PROFILE ===");

        if (!sessionService.isLoggedIn() || !"SELLER".equals(sessionService.getUserRole())) {
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

        return "seller/profile";
    }


    @GetMapping("/test-dashboard")
    public String testSellerDashboard(Model model) {
        System.out.println("=== TESTING SELLER DASHBOARD ===");


        User testSeller = new User("test@seller.com", "password", "Test Seller", "SELLER");
        testSeller.setId(2L);

        model.addAttribute("user", testSeller);
        model.addAttribute("productCount", 5);
        model.addAttribute("lowStockCount", 2);
        model.addAttribute("outOfStockCount", 1);
        model.addAttribute("totalStockValue", 12500.75);

        return "seller/dashboard";
    }
}