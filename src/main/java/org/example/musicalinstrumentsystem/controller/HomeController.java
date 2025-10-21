package org.example.musicalinstrumentsystem.controller;

import org.example.musicalinstrumentsystem.entity.Product;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.repository.UserRepository;
import org.example.musicalinstrumentsystem.service.ProductService;
import org.example.musicalinstrumentsystem.service.SessionService;
import org.example.musicalinstrumentsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserService userService;

    // Admin password
    private final String ADMIN_PASSWORD = "alwis123";

    //  BASIC PAGES

    @GetMapping("/")
    public String home(Model model) {
        System.out.println(" LOADING HOMEPAGE ");
        model.addAttribute("isLoggedIn", sessionService.isLoggedIn());
        model.addAttribute("userRole", sessionService.getUserRole());

        // Add some featured products for the homepage
        if (sessionService.isLoggedIn() && "BUYER".equals(sessionService.getUserRole())) {
            List<Product> featuredProducts = productService.getAvailableProducts();
            if (featuredProducts != null && featuredProducts.size() > 6) {
                featuredProducts = featuredProducts.subList(0, 6);
            }
            model.addAttribute("featuredProducts", featuredProducts);
        }

        return "index";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        System.out.println("=== SHOW REGISTER FORM ===");
        model.addAttribute("isLoggedIn", sessionService.isLoggedIn());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String password,
                               @RequestParam String role,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String address) {

        System.out.println("=== REGISTRATION ATTEMPT ===");
        System.out.println("Name: " + name);
        System.out.println("Email: " + email);
        System.out.println("Role: " + role);

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            System.out.println(" Registration failed: Email already exists");
            return "redirect:/register?error=email_exists";
        }

        // Create new user
        User user = new User(email, password, name, role);
        user.setPhone(phone);
        user.setAddress(address);

        userRepository.save(user);
        System.out.println(" Registration successful for: " + email);
        System.out.println("User ID: " + user.getId());

        return "redirect:/login?success=registered";
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        System.out.println("=== SHOW LOGIN FORM ===");
        model.addAttribute("isLoggedIn", sessionService.isLoggedIn());
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            @RequestParam String role,
                            Model model) {

        System.out.println("=== LOGIN ATTEMPT ===");
        System.out.println("Email: " + email);
        System.out.println("Input Role: " + role);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            System.out.println(" User found in database:");
            System.out.println("   - ID: " + user.getId());
            System.out.println("   - Name: " + user.getName());
            System.out.println("   - DB Role: " + user.getRole());
            System.out.println("   - DB Password: " + user.getPassword());

            boolean passwordMatch = user.getPassword().equals(password);
            boolean roleMatch = user.getRole().equals(role);

            System.out.println("   - Password match: " + passwordMatch);
            System.out.println("   - Role match: " + roleMatch);

            if (passwordMatch && roleMatch) {
                System.out.println(" LOGIN SUCCESSFUL! Redirecting to " + role + " dashboard");

                // Store user in session
                sessionService.login(user);
                model.addAttribute("user", user);

                // Successful login - redirect based on role
                switch (role) {
                    case "BUYER":
                        return "redirect:/buyer/dashboard";
                    case "SELLER":
                        return "redirect:/seller/dashboard";
                    case "STOCK_MANAGER":
                        return "redirect:/stock-manager/dashboard";
                    case "ADMIN":
                        return "redirect:/admin/dashboard";
                    default:
                        return "redirect:/login?error=invalid_role";
                }
            } else {
                System.out.println(" Login failed - Password or role mismatch");
            }
        } else {
            System.out.println(" User not found in database");
        }

        System.out.println(" LOGIN FAILED!");
        return "redirect:/login?error=invalid_credentials";
    }

    @PostMapping("/admin/login")
    public String adminLogin(@RequestParam String adminKey, Model model) {
        System.out.println("=== ADMIN LOGIN ATTEMPT ===");
        System.out.println("Admin key entered: " + adminKey);

        if (ADMIN_PASSWORD.equals(adminKey)) {
            System.out.println(" Admin login successful!");


            User adminUser = new User("admin@musicstore.com", "admin", "Administrator", "ADMIN");
            sessionService.login(adminUser);
            model.addAttribute("user", adminUser);

            return "redirect:/admin/dashboard";
        }

        System.out.println(" Admin login failed!");
        return "redirect:/login?error=invalid_admin";
    }

    @GetMapping("/logout")
    public String logout() {
        String userEmail = sessionService.getCurrentUser() != null ?
                sessionService.getCurrentUser().getEmail() : "Unknown";
        System.out.println("=== LOGOUT ===");
        System.out.println("Logging out user: " + userEmail);

        sessionService.logout();
        return "redirect:/?success=logged_out";
    }

    //ERROR HANDLING

    @GetMapping("/error")
    public String handleError() {
        return "error";
    }

    // ========== TEST ROUTES (For Development) ==========

    @GetMapping("/test/products")
    public String testProducts(Model model) {
        System.out.println("=== TEST: ALL PRODUCTS IN DATABASE ===");

        List<Product> allProducts = productService.getAllProducts();

        if (allProducts == null) {
            allProducts = new ArrayList<>();
        }

        System.out.println("Total products: " + allProducts.size());
        for (Product product : allProducts) {
            System.out.println("Product: " + product.getName() +
                    " | Seller: " + (product.getSeller() != null ? product.getSeller().getName() : "No Seller") +
                    " | Role: " + (product.getSeller() != null ? product.getSeller().getRole() : "No Role"));
        }

        model.addAttribute("products", allProducts);
        return "test/products";
    }

    // HEALTH CHECK

    @GetMapping("/health")
    @ResponseBody
    public String healthCheck() {
        return " Musical Instrument System is running! Status: OK";
    }

    //  TEST DASHBOARDS (Remove these after testing)

    @GetMapping("/test/buyer-dashboard")
    public String testBuyerDashboard(Model model) {
        System.out.println("=== TESTING BUYER DASHBOARD ===");

        // Create a mock buyer for testing
        User testBuyer = new User("test@buyer.com", "password", "Test Buyer", "BUYER");
        testBuyer.setId(1L);

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
        testProduct.setSeller(testBuyer);

        testProducts.add(testProduct);

        List<String> categories = List.of(
                "Guitars", "Pianos & Keyboards", "Drums & Percussion",
                "String Instruments", "Wind Instruments", "Brass Instruments",
                "Accessories", "Amplifiers", "Recording Equipment"
        );

        model.addAttribute("user", testBuyer);
        model.addAttribute("availableProducts", testProducts);
        model.addAttribute("recentProducts", testProducts);
        model.addAttribute("categories", categories);
        model.addAttribute("totalProducts", testProducts.size());

        return "buyer/dashboard";
    }

    @GetMapping("/test/seller-dashboard")
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

    @GetMapping("/test/stockmanager-dashboard")
    public String testStockManagerDashboard(Model model) {
        System.out.println("=== TESTING STOCK MANAGER DASHBOARD ===");


        User testManager = new User("test@stockmanager.com", "password", "Test Stock Manager", "STOCK_MANAGER");
        testManager.setId(3L);

        model.addAttribute("user", testManager);
        model.addAttribute("totalProducts", 25);
        model.addAttribute("lowStockCount", 5);
        model.addAttribute("outOfStockCount", 3);
        model.addAttribute("totalStockValue", 45000.50);

        return "stock-manager/dashboard";
    }
}