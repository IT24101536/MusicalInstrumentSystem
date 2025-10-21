// ProductController.java
package org.example.musicalinstrumentsystem.controller;

import org.example.musicalinstrumentsystem.entity.Product;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.service.FileStorageService;
import org.example.musicalinstrumentsystem.service.ProductService;
import org.example.musicalinstrumentsystem.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/seller/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private FileStorageService fileStorageService;

    //  PRODUCT LIST
    @GetMapping
    public String sellerProducts(Model model) {
        System.out.println("=== ACCESSING SELLER PRODUCTS ===");

        if (!sessionService.isLoggedIn() || !"SELLER".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();
        List<Product> products = productService.getProductsBySeller(currentUser);

        // Calculate statistics
        long totalProducts = products.size();
        long lowStockCount = products.stream().filter(Product::isLowStock).count();
        double totalStockValue = productService.getTotalStockValueBySeller(currentUser);

        model.addAttribute("products", products);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("totalStockValue", totalStockValue);
        model.addAttribute("user", currentUser);

        return "seller/products";
    }

    //  ADD NEW PRODUCT
    @GetMapping("/new")
    public String showAddProductForm(Model model) {
        if (!sessionService.isLoggedIn() || !"SELLER".equals(sessionService.getUserRole())) {
            return "redirect:/login";
        }

        model.addAttribute("product", new Product());
        model.addAttribute("user", sessionService.getCurrentUser());
        model.addAttribute("categories", getInstrumentCategories());
        return "seller/new-product";
    }

    @PostMapping("/create")
    public String createProduct(@RequestParam String name,
                                @RequestParam String category,
                                @RequestParam String brand,
                                @RequestParam String description,
                                @RequestParam Double price,
                                @RequestParam Integer stockQuantity,
                                @RequestParam(required = false) Integer minStockLevel,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(required = false) MultipartFile imageFile) {

        System.out.println("=== CREATING NEW PRODUCT ===");
        System.out.println("Name: " + name);
        System.out.println("Category: " + category);
        System.out.println("Price: " + price);
        System.out.println("Image file uploaded: " + (imageFile != null && !imageFile.isEmpty()));

        if (!sessionService.isLoggedIn() || !"SELLER".equals(sessionService.getUserRole())) {
            return "redirect:/login";
        }

        User currentUser = sessionService.getCurrentUser();

        // Check if product name already exists for this seller
        if (productService.productNameExists(name, currentUser)) {
            return "redirect:/seller/products/new?error=product_exists";
        }

        // Create new product
        Product product = new Product(name, category, brand, description, price, stockQuantity, currentUser);

        if (minStockLevel != null) {
            product.setMinStockLevel(minStockLevel);
        }

        // Handle image upload - prioritize uploaded file over URL
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String savedFilePath = fileStorageService.saveFile(imageFile);
                product.setImageUrl(savedFilePath);
                System.out.println(" Image uploaded: " + savedFilePath);
            } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                product.setImageUrl(imageUrl);
                System.out.println(" Image URL set: " + imageUrl);
            }
        } catch (IOException e) {
            System.err.println(" Failed to upload image: " + e.getMessage());
            return "redirect:/seller/products/new?error=upload_failed";
        }

        productService.createProduct(product);
        System.out.println(" Product created: " + name);

        return "redirect:/seller/products?success=product_created";
    }

    //  EDIT PRODUCT
    @GetMapping("/edit/{id}")
    public String editProductForm(@PathVariable Long id, Model model) {
        System.out.println("=== EDIT PRODUCT FORM ===");
        System.out.println("Product ID: " + id);

        if (!sessionService.isLoggedIn() || !"SELLER".equals(sessionService.getUserRole())) {
            return "redirect:/login";
        }

        User currentUser = sessionService.getCurrentUser();
        Optional<Product> productOpt = productService.getProductById(id);

        if (productOpt.isEmpty()) {
            return "redirect:/seller/products?error=product_not_found";
        }

        Product product = productOpt.get();

        // Check if the product belongs to the current seller
        if (!product.getSeller().getId().equals(currentUser.getId())) {
            return "redirect:/seller/products?error=access_denied";
        }

        model.addAttribute("product", product);
        model.addAttribute("user", currentUser);
        model.addAttribute("categories", getInstrumentCategories());

        return "seller/edit-product";
    }

    @PostMapping("/update")
    public String updateProduct(@RequestParam Long id,
                                @RequestParam String name,
                                @RequestParam String category,
                                @RequestParam String brand,
                                @RequestParam String description,
                                @RequestParam Double price,
                                @RequestParam Integer stockQuantity,
                                @RequestParam Integer minStockLevel,
                                @RequestParam(required = false) String imageUrl,
                                @RequestParam(required = false) MultipartFile imageFile,
                                @RequestParam(required = false) String keepExistingImage) {

        System.out.println("=== UPDATING PRODUCT ===");
        System.out.println("Product ID: " + id);
        System.out.println("Image file uploaded: " + (imageFile != null && !imageFile.isEmpty()));

        if (!sessionService.isLoggedIn() || !"SELLER".equals(sessionService.getUserRole())) {
            return "redirect:/login";
        }

        User currentUser = sessionService.getCurrentUser();
        Optional<Product> productOpt = productService.getProductById(id);

        if (productOpt.isEmpty()) {
            return "redirect:/seller/products?error=product_not_found";
        }

        Product product = productOpt.get();

        // Check if the product belongs to the current seller
        if (!product.getSeller().getId().equals(currentUser.getId())) {
            return "redirect:/seller/products?error=access_denied";
        }

        String oldImageUrl = product.getImageUrl();

        // Update product details
        product.setName(name);
        product.setCategory(category);
        product.setBrand(brand);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setMinStockLevel(minStockLevel);

        // Handle image update
        try {
            if (imageFile != null && !imageFile.isEmpty()) {

                if (oldImageUrl != null && oldImageUrl.startsWith("/uploads/")) {
                    fileStorageService.deleteFile(oldImageUrl);
                }
                String savedFilePath = fileStorageService.saveFile(imageFile);
                product.setImageUrl(savedFilePath);
                System.out.println(" Image updated: " + savedFilePath);
            } else if (imageUrl != null && !imageUrl.trim().isEmpty() && !"on".equals(keepExistingImage)) {

                if (oldImageUrl != null && oldImageUrl.startsWith("/uploads/")) {
                    fileStorageService.deleteFile(oldImageUrl);
                }
                product.setImageUrl(imageUrl);
                System.out.println(" Image URL updated: " + imageUrl);
            }

        } catch (IOException e) {
            System.err.println(" Failed to upload image: " + e.getMessage());
            return "redirect:/seller/products/edit/" + id + "?error=upload_failed";
        }

        productService.updateProduct(product);
        System.out.println(" Product updated: " + name);

        return "redirect:/seller/products?success=product_updated";
    }

    //  DELETE PRODUCT
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        System.out.println("=== DELETING PRODUCT ===");
        System.out.println("Product ID: " + id);

        if (!sessionService.isLoggedIn() || !"SELLER".equals(sessionService.getUserRole())) {
            return "redirect:/login";
        }

        User currentUser = sessionService.getCurrentUser();
        Optional<Product> productOpt = productService.getProductById(id);

        if (productOpt.isEmpty()) {
            return "redirect:/seller/products?error=product_not_found";
        }

        Product product = productOpt.get();

        // Check if the product belongs to the current seller
        if (!product.getSeller().getId().equals(currentUser.getId())) {
            return "redirect:/seller/products?error=access_denied";
        }

        try {
            // Delete associated image file if it's local
            String imageUrl = product.getImageUrl();
            if (imageUrl != null && imageUrl.startsWith("/uploads/")) {
                fileStorageService.deleteFile(imageUrl);
            }

            boolean deleted = productService.deleteProduct(id);
            if (deleted) {
                System.out.println(" Product deleted: " + product.getName());
                return "redirect:/seller/products?success=product_deleted";
            } else {
                System.out.println(" Product deletion failed: " + product.getName());
                return "redirect:/seller/products?error=delete_failed";
            }
        } catch (Exception e) {
            System.out.println(" Cannot delete product: " + e.getMessage());
            // Check if it's a foreign key constraint violation
            if (e.getMessage() != null && e.getMessage().contains("foreign key constraint")) {
                return "redirect:/seller/products?error=product_has_orders";
            }
            return "redirect:/seller/products?error=delete_failed";
        }
    }

    //  UPDATE STOCK
    @PostMapping("/update-stock")
    public String updateStock(@RequestParam Long productId,
                              @RequestParam Integer newQuantity) {
        System.out.println("=== UPDATING STOCK ===");
        System.out.println("Product ID: " + productId + ", New Quantity: " + newQuantity);

        if (!sessionService.isLoggedIn() || !"SELLER".equals(sessionService.getUserRole())) {
            return "redirect:/login";
        }

        User currentUser = sessionService.getCurrentUser();
        Optional<Product> productOpt = productService.getProductById(productId);

        if (productOpt.isEmpty()) {
            return "redirect:/seller/products?error=product_not_found";
        }

        Product product = productOpt.get();

        // Check if the product belongs to the current seller
        if (!product.getSeller().getId().equals(currentUser.getId())) {
            return "redirect:/seller/products?error=access_denied";
        }

        productService.updateStock(productId, newQuantity);
        System.out.println(" Stock updated for: " + product.getName());

        return "redirect:/seller/products?success=stock_updated";
    }

    //  HELPER METHODS
    private List<String> getInstrumentCategories() {
        return List.of(
                "Guitars",
                "Pianos & Keyboards",
                "Drums & Percussion",
                "String Instruments",
                "Wind Instruments",
                "Brass Instruments",
                "Recording Equipment",
                "Amplifiers & Effects",
                "Accessories",
                "Other"
        );
    }
}