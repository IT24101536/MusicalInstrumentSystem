package org.example.musicalinstrumentsystem.repository;

import org.example.musicalinstrumentsystem.entity.Product;
import org.example.musicalinstrumentsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find products by seller
    List<Product> findBySeller(User seller);

    // Find products by category
    List<Product> findByCategory(String category);

    // Find products by brand
    List<Product> findByBrand(String brand);

    // Find low stock products (stock <= minStockLevel AND stock > 0)
    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= p.minStockLevel AND p.stockQuantity > 0")
    List<Product> findLowStockProducts();

    // Find out of stock products (stock = 0)
    @Query("SELECT p FROM Product p WHERE p.stockQuantity = 0")
    List<Product> findOutOfStockProducts();

    // Find products with stock greater than specified quantity
    List<Product> findByStockQuantityGreaterThan(Integer quantity);

    // Search products by name (case insensitive)
    List<Product> findByNameContainingIgnoreCase(String name);

    // Find products by seller and category
    List<Product> findBySellerAndCategory(User seller, String category);

    // Check if product name exists for a specific seller
    boolean existsByNameAndSeller(String name, User seller);

    // Find products by price range
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice")
    List<Product> findByPriceBetween(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);

    // Find products by category and price range
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.price BETWEEN :minPrice AND :maxPrice")
    List<Product> findByCategoryAndPriceBetween(@Param("category") String category,
                                                @Param("minPrice") Double minPrice,
                                                @Param("maxPrice") Double maxPrice);

    // Count products by seller
    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller = :seller")
    Long countBySeller(@Param("seller") User seller);

    // Count products by category
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category = :category")
    Long countByCategory(@Param("category") String category);

    // Find products with pagination support
    @Query("SELECT p FROM Product p ORDER BY p.createdAt DESC")
    List<Product> findAllOrderByCreatedAtDesc();

    // Find products by seller with pagination support
    @Query("SELECT p FROM Product p WHERE p.seller = :seller ORDER BY p.createdAt DESC")
    List<Product> findBySellerOrderByCreatedAtDesc(@Param("seller") User seller);

    // FIXED: Find recently added products
    @Query(value = "SELECT * FROM products WHERE created_at >= DATE_SUB(NOW(), INTERVAL :days DAY) ORDER BY created_at DESC", nativeQuery = true)
    List<Product> findRecentProducts(@Param("days") Integer days);

    // Get total stock value by seller
    @Query("SELECT SUM(p.price * p.stockQuantity) FROM Product p WHERE p.seller = :seller")
    Double getTotalStockValueBySeller(@Param("seller") User seller);

    // Get average price by category
    @Query("SELECT AVG(p.price) FROM Product p WHERE p.category = :category")
    Double getAveragePriceByCategory(@Param("category") String category);

    // Find products that need restocking
    @Query("SELECT p FROM Product p WHERE p.stockQuantity < p.minStockLevel ORDER BY p.stockQuantity ASC")
    List<Product> findProductsNeedingRestock();

    // Custom query to find products with images
    @Query("SELECT p FROM Product p WHERE p.imageUrl IS NOT NULL AND p.imageUrl != ''")
    List<Product> findProductsWithImages();

    // Find products by multiple categories
    @Query("SELECT p FROM Product p WHERE p.category IN :categories")
    List<Product> findByCategories(@Param("categories") List<String> categories);

    // Search products by name or description
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Product> searchByNameOrDescription(@Param("query") String query);

    // Find products with high stock value (price * quantity)
    @Query("SELECT p FROM Product p WHERE (p.price * p.stockQuantity) > :minValue ORDER BY (p.price * p.stockQuantity) DESC")
    List<Product> findHighValueProducts(@Param("minValue") Double minValue);
}