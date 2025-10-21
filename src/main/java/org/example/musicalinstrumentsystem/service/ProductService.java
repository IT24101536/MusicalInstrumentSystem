package org.example.musicalinstrumentsystem.service;

import org.example.musicalinstrumentsystem.entity.Product;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // CRUD OPERATIONS

    // Create
    public Product createProduct(Product product) {
        try {
            System.out.println("=== CREATING PRODUCT ===");
            System.out.println("Product Name: " + (product != null ? product.getName() : "NULL"));

            if (product == null) {
                System.out.println(" Product is null");
                return null;
            }

            Product savedProduct = productRepository.save(product);
            System.out.println(" Product created successfully: " + savedProduct.getName());
            return savedProduct;

        } catch (Exception e) {
            System.out.println(" ERROR creating product: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Read All products
    public List<Product> getAllProducts() {
        try {
            System.out.println("=== FETCHING ALL PRODUCTS ===");
            List<Product> products = productRepository.findAll();
            System.out.println(" Found " + (products != null ? products.size() : 0) + " products");
            return products != null ? products : new ArrayList<>();

        } catch (Exception e) {
            System.out.println(" ERROR fetching all products: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public Optional<Product> getProductById(Long id) {
        try {
            System.out.println("=== FETCHING PRODUCT BY ID: " + id + " ===");

            if (id == null || id <= 0) {
                System.out.println(" Invalid product ID: " + id);
                return Optional.empty();
            }

            Optional<Product> product = productRepository.findById(id);
            if (product.isPresent()) {
                System.out.println(" Product found: " + product.get().getName());
            } else {
                System.out.println(" Product not found with ID: " + id);
            }
            return product;

        } catch (Exception e) {
            System.out.println(" ERROR fetching product by ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public List<Product> getProductsBySeller(User seller) {
        try {
            System.out.println("=== FETCHING PRODUCTS BY SELLER ===");
            System.out.println("Seller: " + (seller != null ? seller.getName() : "NULL"));

            if (seller == null || seller.getId() == null) {
                System.out.println(" Invalid seller provided");
                return new ArrayList<>();
            }

            List<Product> products = productRepository.findBySeller(seller);
            System.out.println(" Found " + (products != null ? products.size() : 0) + " products for seller");
            return products != null ? products : new ArrayList<>();

        } catch (Exception e) {
            System.out.println(" ERROR fetching products by seller: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Product> getProductsByCategory(String category) {
        try {
            System.out.println("=== FETCHING PRODUCTS BY CATEGORY: " + category + " ===");

            if (category == null || category.trim().isEmpty()) {
                System.out.println(" Invalid category provided");
                return new ArrayList<>();
            }

            List<Product> products = productRepository.findByCategory(category);
            System.out.println(" Found " + (products != null ? products.size() : 0) + " products in category: " + category);
            return products != null ? products : new ArrayList<>();

        } catch (Exception e) {
            System.out.println(" ERROR fetching products by category: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Product> getLowStockProducts() {
        try {
            System.out.println("=== FETCHING LOW STOCK PRODUCTS ===");
            List<Product> lowStockProducts = productRepository.findLowStockProducts();
            System.out.println(" Found " + (lowStockProducts != null ? lowStockProducts.size() : 0) + " low stock products");
            return lowStockProducts != null ? lowStockProducts : new ArrayList<>();

        } catch (Exception e) {
            System.out.println(" ERROR fetching low stock products: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Product> getAvailableProducts() {
        try {
            System.out.println("=== FETCHING AVAILABLE PRODUCTS ===");
            List<Product> availableProducts = productRepository.findByStockQuantityGreaterThan(0);
            System.out.println(" Found " + (availableProducts != null ? availableProducts.size() : 0) + " available products");
            return availableProducts != null ? availableProducts : new ArrayList<>();

        } catch (Exception e) {
            System.out.println(" ERROR fetching available products: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Product> getRecentProducts(Integer days) {
        try {
            System.out.println("=== FETCHING RECENT PRODUCTS (LAST " + days + " DAYS) ===");

            if (days == null || days <= 0) {
                System.out.println(" Invalid days parameter: " + days);
                return new ArrayList<>();
            }

            List<Product> recentProducts = productRepository.findRecentProducts(days);
            System.out.println(" Found " + (recentProducts != null ? recentProducts.size() : 0) + " recent products from last " + days + " days");
            return recentProducts != null ? recentProducts : new ArrayList<>();

        } catch (Exception e) {
            System.out.println(" ERROR fetching recent products: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    @Transactional
    public Product updateProduct(Product product) {
        try {
            System.out.println("=== UPDATING PRODUCT ===");
            System.out.println("Product ID: " + (product != null ? product.getId() : "NULL"));
            System.out.println("Product Name: " + (product != null ? product.getName() : "NULL"));

            if (product == null || product.getId() == null) {
                System.out.println(" Invalid product for update");
                return null;
            }

            Optional<Product> existingProduct = productRepository.findById(product.getId());
            if (existingProduct.isEmpty()) {
                System.out.println(" Product not found for update: " + product.getId());
                return null;
            }

            Product updatedProduct = productRepository.save(product);
            System.out.println(" Product updated successfully in database: " + updatedProduct.getName());
            System.out.println("Updated Product - ID: " + updatedProduct.getId() + ", Name: " + updatedProduct.getName() + ", Price: $" + updatedProduct.getPrice());
            return updatedProduct;

        } catch (Exception e) {
            System.out.println(" ERROR updating product: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    @Transactional
    public Product updateStock(Long productId, Integer newQuantity) {
        try {
            System.out.println("=== UPDATING STOCK ===");
            System.out.println("Product ID: " + productId);
            System.out.println("New Quantity: " + newQuantity);

            if (productId == null || productId <= 0) {
                System.out.println(" Invalid product ID");
                return null;
            }

            if (newQuantity == null || newQuantity < 0) {
                System.out.println(" Invalid quantity: " + newQuantity);
                return null;
            }

            Optional<Product> productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty()) {
                System.out.println(" Product not found with ID: " + productId);
                return null;
            }

            Product product = productOpt.get();
            product.setStockQuantity(newQuantity);

            Product updatedProduct = productRepository.save(product);
            System.out.println(" Stock updated successfully for: " + updatedProduct.getName());
            return updatedProduct;

        } catch (Exception e) {
            System.out.println(" ERROR updating stock: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Delete
    @Transactional
    public boolean deleteProduct(Long id) {
        System.out.println("=== DELETING PRODUCT ===");
        System.out.println("Product ID: " + id);

        if (id == null || id <= 0) {
            System.out.println(" Invalid product ID for deletion");
            return false;
        }

        // Check if product exists
        Optional<Product> product = productRepository.findById(id);
        if (product.isEmpty()) {
            System.out.println(" Product not found for deletion: " + id);
            return false;
        }

        productRepository.deleteById(id);
        System.out.println(" Product deleted successfully from database: " + product.get().getName());
        System.out.println("Deleted Product - ID: " + id + ", Name: " + product.get().getName());
        return true;
    }


    // Check if product name
    public boolean productNameExists(String name, User seller) {
        try {
            System.out.println("=== CHECKING PRODUCT NAME EXISTS ===");
            System.out.println("Product Name: " + name);
            System.out.println("Seller: " + (seller != null ? seller.getName() : "NULL"));

            if (name == null || name.trim().isEmpty() || seller == null || seller.getId() == null) {
                System.out.println(" Invalid parameters for product name check");
                return false;
            }

            boolean exists = productRepository.existsByNameAndSeller(name, seller);
            System.out.println(" Product name exists check: " + exists);
            return exists;

        } catch (Exception e) {
            System.out.println(" ERROR checking product name exists: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Search product
    public List<Product> searchProducts(String query) {
        try {
            System.out.println("=== SEARCHING PRODUCTS ===");
            System.out.println("Query: " + query);

            if (query == null || query.trim().isEmpty()) {
                System.out.println(" Empty search query");
                return new ArrayList<>();
            }

            List<Product> searchResults = productRepository.findByNameContainingIgnoreCase(query);
            System.out.println(" Found " + (searchResults != null ? searchResults.size() : 0) + " search results");
            return searchResults != null ? searchResults : new ArrayList<>();

        } catch (Exception e) {
            System.out.println(" ERROR searching products: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    public long getProductCountBySeller(User seller) {
        try {
            System.out.println("=== COUNTING PRODUCTS BY SELLER ===");
            System.out.println("Seller: " + (seller != null ? seller.getName() : "NULL"));

            if (seller == null || seller.getId() == null) {
                System.out.println(" Invalid seller for product count");
                return 0;
            }

            List<Product> products = productRepository.findBySeller(seller);
            long count = products != null ? products.size() : 0;
            System.out.println(" Product count for seller: " + count);
            return count;

        } catch (Exception e) {
            System.out.println(" ERROR counting products by seller: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    public double getTotalStockValueBySeller(User seller) {
        try {
            System.out.println("=== CALCULATING TOTAL STOCK VALUE ===");
            System.out.println("Seller: " + (seller != null ? seller.getName() : "NULL"));

            if (seller == null || seller.getId() == null) {
                System.out.println(" Invalid seller for stock value calculation");
                return 0.0;
            }

            List<Product> products = productRepository.findBySeller(seller);
            if (products == null || products.isEmpty()) {
                System.out.println(" No products found for seller, stock value: $0.00");
                return 0.0;
            }

            double totalValue = products.stream()
                    .filter(p -> p != null && p.getPrice() != null && p.getStockQuantity() != null)
                    .mapToDouble(p -> p.getPrice() * p.getStockQuantity())
                    .sum();

            System.out.println(" Total stock value for seller: $" + String.format("%.2f", totalValue));
            return totalValue;

        } catch (Exception e) {
            System.out.println(" ERROR calculating total stock value: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    public double getTotalSystemStockValue() {
        try {
            System.out.println("=== CALCULATING TOTAL SYSTEM STOCK VALUE ===");
            List<Product> allProducts = productRepository.findAll();

            if (allProducts == null || allProducts.isEmpty()) {
                System.out.println(" No products in system, total value: $0.00");
                return 0.0;
            }

            double totalValue = allProducts.stream()
                    .filter(p -> p != null && p.getPrice() != null && p.getStockQuantity() != null)
                    .mapToDouble(p -> p.getPrice() * p.getStockQuantity())
                    .sum();

            System.out.println(" Total system stock value: $" + String.format("%.2f", totalValue));
            return totalValue;

        } catch (Exception e) {
            System.out.println(" ERROR calculating total system stock value: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    public List<Product> getProductsBySellerAndCategory(User seller, String category) {
        try {
            System.out.println("=== FETCHING PRODUCTS BY SELLER AND CATEGORY ===");
            System.out.println("Seller: " + (seller != null ? seller.getName() : "NULL"));
            System.out.println("Category: " + category);

            if (seller == null || seller.getId() == null || category == null || category.trim().isEmpty()) {
                System.out.println(" Invalid parameters for seller/category search");
                return new ArrayList<>();
            }

            List<Product> products = productRepository.findBySellerAndCategory(seller, category);
            System.out.println(" Found " + (products != null ? products.size() : 0) + " products for seller and category");
            return products != null ? products : new ArrayList<>();

        } catch (Exception e) {
            System.out.println(" ERROR fetching products by seller and category: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Product> getAllProductsSafe() {
        return getAllProducts();
    }

    public List<Product> getLowStockProductsSafe() {
        return getLowStockProducts();
    }

    public boolean isValidProduct(Product product) {
        if (product == null) return false;
        if (product.getName() == null || product.getName().trim().isEmpty()) return false;
        if (product.getPrice() == null || product.getPrice() < 0) return false;
        if (product.getStockQuantity() == null || product.getStockQuantity() < 0) return false;
        if (product.getCategory() == null || product.getCategory().trim().isEmpty()) return false;
        if (product.getBrand() == null || product.getBrand().trim().isEmpty()) return false;
        if (product.getSeller() == null) return false;
        return true;
    }
}