package org.example.musicalinstrumentsystem.controller;

import org.example.musicalinstrumentsystem.entity.Product;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.repository.UserRepository;
import org.example.musicalinstrumentsystem.service.EmailService;
import org.example.musicalinstrumentsystem.service.FileStorageService;
import org.example.musicalinstrumentsystem.service.ProductService;
import org.example.musicalinstrumentsystem.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/stock-manager")
public class StockManagementController {

    @Autowired
    private ProductService productService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private EmailService emailService;

    //  STOCK MANAGER DASHBOARD
    @GetMapping("/dashboard")
    public String stockManagerDashboard(Model model) {
        System.out.println("=== ACCESSING STOCK MANAGER DASHBOARD ===");

        if (!sessionService.isLoggedIn()) {
            System.out.println("❌ Access denied: Not logged in");
            return "redirect:/login?error=access_denied";
        }

        if (!"STOCK_MANAGER".equals(sessionService.getUserRole())) {
            System.out.println("❌ Access denied: User role is " + sessionService.getUserRole() + ", expected STOCK_MANAGER");
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();
        System.out.println("✅ Access granted for stock manager: " + currentUser.getEmail());

        try {
            // Add stock management statistics
            List<Product> allProducts = productService.getAllProducts();
            List<Product> lowStockProducts = productService.getLowStockProducts();

            long totalProducts = allProducts.size();
            long lowStockCount = lowStockProducts.size();
            long outOfStockCount = allProducts.stream().filter(p -> p.getStockQuantity() == 0).count();
            double totalStockValue = allProducts.stream()
                    .mapToDouble(p -> p.getPrice() * p.getStockQuantity())
                    .sum();

            model.addAttribute("user", currentUser);
            model.addAttribute("products", allProducts);
            model.addAttribute("lowStockProducts", lowStockProducts);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("lowStockCount", lowStockCount);
            model.addAttribute("outOfStockCount", outOfStockCount);
            model.addAttribute("totalStockValue", totalStockValue);

            return "stock-manager/dashboard";

        } catch (Exception e) {
            System.out.println("❌ ERROR in stockManagerDashboard: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Failed to load dashboard data");
            return "stock-manager/dashboard";
        }
    }

    //  VIEW ALL PRODUCTS
    @GetMapping("/products")
    public String viewAllProducts(Model model) {
        System.out.println("=== STOCK MANAGER VIEWING ALL PRODUCTS ===");

        if (!sessionService.isLoggedIn() || !"STOCK_MANAGER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            List<Product> allProducts = productService.getAllProducts();
            List<Product> lowStockProducts = productService.getLowStockProducts();


            long inStockCount = 0;
            long outOfStockCount = 0;
            long categoryCount = 0;
            long sellerCount = 0;

            if (allProducts != null && !allProducts.isEmpty()) {
                inStockCount = allProducts.stream()
                        .filter(p -> p != null && !p.isLowStock() && p.getStockQuantity() > 0)
                        .count();
                outOfStockCount = allProducts.stream()
                        .filter(p -> p != null && p.getStockQuantity() == 0)
                        .count();
                categoryCount = allProducts.stream()
                        .filter(p -> p != null && p.getCategory() != null)
                        .map(Product::getCategory)
                        .distinct()
                        .count();
                sellerCount = allProducts.stream()
                        .filter(p -> p != null && p.getSeller() != null && p.getSeller().getName() != null)
                        .map(product -> product.getSeller().getName())
                        .distinct()
                        .count();
            }

            model.addAttribute("products", allProducts != null ? allProducts : List.of());
            model.addAttribute("lowStockProducts", lowStockProducts != null ? lowStockProducts : List.of());
            model.addAttribute("inStockCount", inStockCount);
            model.addAttribute("outOfStockCount", outOfStockCount);
            model.addAttribute("categoryCount", categoryCount);
            model.addAttribute("sellerCount", sellerCount);
            model.addAttribute("user", sessionService.getCurrentUser());

            return "stock-manager/products";

        } catch (Exception e) {
            System.out.println("❌ ERROR in viewAllProducts: " + e.getMessage());
            e.printStackTrace();

            // Return safe default values on error
            model.addAttribute("products", List.of());
            model.addAttribute("lowStockProducts", List.of());
            model.addAttribute("inStockCount", 0);
            model.addAttribute("outOfStockCount", 0);
            model.addAttribute("categoryCount", 0);
            model.addAttribute("sellerCount", 0);
            model.addAttribute("user", sessionService.getCurrentUser());
            model.addAttribute("error", "Failed to load products data");

            return "stock-manager/products";
        }
    }

    // ADD NEW PRODUCT
    @GetMapping("/products/new")
    public String showAddProductForm(Model model) {
        System.out.println("=== STOCK MANAGER ADDING NEW PRODUCT ===");

        if (!sessionService.isLoggedIn() || !"STOCK_MANAGER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            // Create empty product for form
            Product product = new Product();
            model.addAttribute("product", product);
            model.addAttribute("user", sessionService.getCurrentUser());

            // Add categories for dropdown
            List<String> categories = Arrays.asList(
                    "Guitars", "Pianos & Keyboards", "Drums & Percussion",
                    "String Instruments", "Wind Instruments", "Brass Instruments",
                    "Accessories", "Amplifiers", "Recording Equipment", "Other"
            );
            model.addAttribute("categories", categories);

            // Get all sellers for dropdown
            List<User> sellers = userRepository.findAll().stream()
                    .filter(user -> "SELLER".equals(user.getRole()))
                    .collect(Collectors.toList());
            model.addAttribute("sellers", sellers);

            return "stock-manager/add-product";

        } catch (Exception e) {
            System.out.println("❌ ERROR in showAddProductForm: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/stock-manager/products?error=server_error";
        }
    }

    @PostMapping("/products/create")
    public String createProduct(@RequestParam String name,
                                @RequestParam String description,
                                @RequestParam Double price,
                                @RequestParam Integer stockQuantity,
                                @RequestParam Integer minStockLevel,
                                @RequestParam String category,
                                @RequestParam String brand,
                                @RequestParam Long sellerId,
                                @RequestParam(required = false) MultipartFile imageFile) {

        System.out.println("=== STOCK MANAGER CREATING NEW PRODUCT ===");
        System.out.println("Product Name: " + name);
        System.out.println("Seller ID: " + sellerId);
        System.out.println("Price: " + price);
        System.out.println("Image file uploaded: " + (imageFile != null && !imageFile.isEmpty()));

        if (!sessionService.isLoggedIn() || !"STOCK_MANAGER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            // Validate input
            if (price <= 0) {
                System.out.println("❌ Invalid price: " + price);
                return "redirect:/stock-manager/products/new?error=invalid_price";
            }

            if (stockQuantity < 0) {
                System.out.println("❌ Invalid stock quantity: " + stockQuantity);
                return "redirect:/stock-manager/products/new?error=invalid_stock";
            }

            if (minStockLevel < 1) {
                System.out.println("❌ Invalid minimum stock level: " + minStockLevel);
                return "redirect:/stock-manager/products/new?error=invalid_min_stock";
            }

            // Find the seller
            Optional<User> sellerOpt = userRepository.findById(sellerId);
            if (sellerOpt.isEmpty() || !"SELLER".equals(sellerOpt.get().getRole())) {
                System.out.println("❌ Invalid seller ID: " + sellerId);
                return "redirect:/stock-manager/products/new?error=invalid_seller";
            }

            User seller = sellerOpt.get();

            // Check if product name already exists for this seller
            if (productService.productNameExists(name, seller)) {
                System.out.println("❌ Product name already exists for seller: " + name);
                return "redirect:/stock-manager/products/new?error=product_exists";
            }

            // Create new product
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setStockQuantity(stockQuantity);
            product.setMinStockLevel(minStockLevel);
            product.setCategory(category);
            product.setBrand(brand);
            product.setSeller(seller);

            // Handle image upload
            try {
                if (imageFile != null && !imageFile.isEmpty()) {
                    String savedFilePath = fileStorageService.saveFile(imageFile);
                    product.setImageUrl(savedFilePath);
                    System.out.println("✅ Image uploaded: " + savedFilePath);
                }
            } catch (IOException e) {
                System.err.println("❌ Failed to upload image: " + e.getMessage());
                return "redirect:/stock-manager/products/new?error=upload_failed";
            }

            Product savedProduct = productService.createProduct(product);

            if (savedProduct != null) {
                System.out.println("✅ Stock Manager created new product: " + savedProduct.getName() + " for seller: " + seller.getName());
                return "redirect:/stock-manager/products?success=product_created";
            } else {
                System.out.println("❌ Failed to create product");
                return "redirect:/stock-manager/products/new?error=create_failed";
            }

        } catch (Exception e) {
            System.out.println("❌ ERROR in createProduct: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/stock-manager/products/new?error=server_error";
        }
    }

    // UPDATE STOCK
    @GetMapping("/products/edit/{id}")
    public String editProductStock(@PathVariable Long id, Model model) {
        System.out.println("=== STOCK MANAGER EDITING PRODUCT STOCK ===");

        if (!sessionService.isLoggedIn() || !"STOCK_MANAGER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            Optional<Product> productOpt = productService.getProductById(id);
            if (productOpt.isEmpty()) {
                return "redirect:/stock-manager/products?error=product_not_found";
            }

            Product product = productOpt.get();
            model.addAttribute("product", product);
            model.addAttribute("user", sessionService.getCurrentUser());
            return "stock-manager/edit-product-stock";

        } catch (Exception e) {
            System.out.println("❌ ERROR in editProductStock: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/stock-manager/products?error=server_error";
        }
    }

    @PostMapping("/products/update-stock")
    public String updateProductStock(@RequestParam Long productId,
                                     @RequestParam Integer stockQuantity,
                                     @RequestParam Integer minStockLevel) {
        System.out.println("=== STOCK MANAGER UPDATING STOCK ===");
        System.out.println("Product ID: " + productId);
        System.out.println("Stock Quantity: " + stockQuantity);
        System.out.println("Min Stock Level: " + minStockLevel);

        if (!sessionService.isLoggedIn() || !"STOCK_MANAGER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            Optional<Product> productOpt = productService.getProductById(productId);
            if (productOpt.isEmpty()) {
                return "redirect:/stock-manager/products?error=product_not_found";
            }

            Product product = productOpt.get();

            int previousStock = product.getStockQuantity();
            

            product.setStockQuantity(stockQuantity);
            product.setMinStockLevel(minStockLevel);
            Product updatedProduct = productService.updateProduct(product);

            if (updatedProduct != null) {
                System.out.println("✅ Stock Manager updated stock for: " + product.getName());

                final int LOW_STOCK_THRESHOLD = 5;
                

                if (previousStock > LOW_STOCK_THRESHOLD && stockQuantity <= LOW_STOCK_THRESHOLD) {
                    System.out.println("📧 Low stock threshold crossed! Sending email alert...");
                    System.out.println("   Previous stock: " + previousStock + " → New stock: " + stockQuantity);
                    
                    try {
                        if (stockQuantity == 0) {
                            // Send critical out of stock alert
                            emailService.sendOutOfStockAlert(updatedProduct);
                            System.out.println("✅ Out of stock alert email sent to admin");
                        } else {
                            // Send low stock alert
                            emailService.sendLowStockAlert(updatedProduct, previousStock, stockQuantity);
                            System.out.println("✅ Low stock alert email sent to admin");
                        }
                    } catch (Exception emailException) {
                        System.err.println("⚠️ Failed to send email alert: " + emailException.getMessage());
                        emailException.printStackTrace();

                    }
                }

                else if (previousStock > 0 && stockQuantity == 0 && previousStock <= LOW_STOCK_THRESHOLD) {
                    System.out.println("📧 Stock depleted! Sending out of stock alert...");
                    try {
                        emailService.sendOutOfStockAlert(updatedProduct);
                        System.out.println("✅ Out of stock alert email sent to admin");
                    } catch (Exception emailException) {
                        System.err.println("⚠️ Failed to send email alert: " + emailException.getMessage());
                        emailException.printStackTrace();
                    }
                }
                
                return "redirect:/stock-manager/products?success=stock_updated";
            } else {
                System.out.println("❌ Failed to update stock for: " + product.getName());
                return "redirect:/stock-manager/products?error=update_failed";
            }

        } catch (Exception e) {
            System.out.println("❌ ERROR in updateProductStock: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/stock-manager/products?error=update_failed";
        }
    }

    //  LOW STOCK ALERTS
    @GetMapping("/alerts")
    public String viewLowStockAlerts(Model model) {
        System.out.println("=== VIEWING LOW STOCK ALERTS ===");

        if (!sessionService.isLoggedIn() || !"STOCK_MANAGER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            List<Product> lowStockProducts = productService.getLowStockProducts();
            List<Product> outOfStockProducts = productService.getAllProducts().stream()
                    .filter(p -> p != null && p.getStockQuantity() == 0)
                    .collect(Collectors.toList());

            model.addAttribute("lowStockProducts", lowStockProducts != null ? lowStockProducts : List.of());
            model.addAttribute("outOfStockProducts", outOfStockProducts);
            model.addAttribute("user", sessionService.getCurrentUser());

            return "stock-manager/alerts";

        } catch (Exception e) {
            System.out.println("❌ ERROR in viewLowStockAlerts: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("lowStockProducts", List.of());
            model.addAttribute("outOfStockProducts", List.of());
            model.addAttribute("user", sessionService.getCurrentUser());
            model.addAttribute("error", "Failed to load alerts data");

            return "stock-manager/alerts";
        }
    }

    @GetMapping("/reports")
    public String stockReports(Model model) {
        System.out.println("=== GENERATING STOCK REPORTS ===");

        if (!sessionService.isLoggedIn() || !"STOCK_MANAGER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            List<Product> allProducts = productService.getAllProducts();

            long totalProducts = allProducts != null ? allProducts.size() : 0;
            long lowStockCount = allProducts != null ? allProducts.stream().filter(p -> p != null && p.isLowStock()).count() : 0;
            long outOfStockCount = allProducts != null ? allProducts.stream().filter(p -> p != null && p.getStockQuantity() == 0).count() : 0;
            double totalStockValue = allProducts != null ? allProducts.stream()
                    .filter(p -> p != null)
                    .mapToDouble(p -> p.getPrice() * p.getStockQuantity())
                    .sum() : 0.0;

            // Products by category
            long guitarCount = allProducts != null ? allProducts.stream().filter(p -> p != null && "Guitars".equals(p.getCategory())).count() : 0;
            long pianoCount = allProducts != null ? allProducts.stream().filter(p -> p != null && "Pianos & Keyboards".equals(p.getCategory())).count() : 0;
            long drumsCount = allProducts != null ? allProducts.stream().filter(p -> p != null && "Drums & Percussion".equals(p.getCategory())).count() : 0;

            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("lowStockCount", lowStockCount);
            model.addAttribute("outOfStockCount", outOfStockCount);
            model.addAttribute("totalStockValue", totalStockValue);
            model.addAttribute("guitarCount", guitarCount);
            model.addAttribute("pianoCount", pianoCount);
            model.addAttribute("drumsCount", drumsCount);
            model.addAttribute("user", sessionService.getCurrentUser());

            return "stock-manager/reports";

        } catch (Exception e) {
            System.out.println("❌ ERROR in stockReports: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("totalProducts", 0);
            model.addAttribute("lowStockCount", 0);
            model.addAttribute("outOfStockCount", 0);
            model.addAttribute("totalStockValue", 0.0);
            model.addAttribute("guitarCount", 0);
            model.addAttribute("pianoCount", 0);
            model.addAttribute("drumsCount", 0);
            model.addAttribute("user", sessionService.getCurrentUser());
            model.addAttribute("error", "Failed to load reports data");

            return "stock-manager/reports";
        }
    }

    //  STOCK MANAGER PROFILE
    @GetMapping("/profile")
    public String stockManagerProfile(Model model) {
        if (!sessionService.isLoggedIn() || !"STOCK_MANAGER".equals(sessionService.getUserRole())) {
            return "redirect:/login";
        }
        model.addAttribute("user", sessionService.getCurrentUser());
        return "stock-manager/profile";
    }
}