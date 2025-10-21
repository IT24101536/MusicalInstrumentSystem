package org.example.musicalinstrumentsystem.service;

import org.example.musicalinstrumentsystem.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${spring.mail.username}")
    private String fromEmail;


    public void sendLowStockAlert(Product product, int previousStock, int newStock) {
        try {
            System.out.println("=== SENDING LOW STOCK ALERT EMAIL ===");
            System.out.println("Product: " + product.getName());
            System.out.println("Previous Stock: " + previousStock);
            System.out.println("New Stock: " + newStock);
            System.out.println("Admin Email: " + adminEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(adminEmail);
            message.setSubject("⚠️ Low Stock Alert - " + product.getName());
            
            String emailBody = buildLowStockEmailBody(product, previousStock, newStock);
            message.setText(emailBody);

            mailSender.send(message);
            System.out.println("✅ Low stock alert email sent successfully");

        } catch (Exception e) {
            System.err.println("❌ Failed to send low stock alert email: " + e.getMessage());
            e.printStackTrace();
            // Don't throw exception - we don't want email failure to block stock updates
        }
    }

    private String buildLowStockEmailBody(Product product, int previousStock, int newStock) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        String timestamp = LocalDateTime.now().format(formatter);

        StringBuilder body = new StringBuilder();
        body.append("LOW STOCK ALERT - IMMEDIATE ATTENTION REQUIRED\n");
        body.append("=====================================\n\n");
        
        body.append("Dear Administrator,\n\n");
        body.append("This is an automated alert from the Musical Instrument System.\n");
        body.append("The following product has reached or fallen below the minimum stock threshold.\n\n");
        
        body.append("PRODUCT INFORMATION:\n");
        body.append("-------------------\n");
        body.append("Product Name: ").append(product.getName()).append("\n");
        body.append("Product ID: ").append(product.getId()).append("\n");
        body.append("Category: ").append(product.getCategory()).append("\n");
        body.append("Brand: ").append(product.getBrand()).append("\n");
        body.append("Price: $").append(String.format("%.2f", product.getPrice())).append("\n\n");
        
        body.append("STOCK INFORMATION:\n");
        body.append("-------------------\n");
        body.append("Previous Stock: ").append(previousStock).append(" units\n");
        body.append("Current Stock: ").append(newStock).append(" units\n");
        body.append("Minimum Threshold: ").append(product.getMinStockLevel()).append(" units\n");
        body.append("Stock Change: ").append(newStock - previousStock).append(" units\n\n");

        if (newStock == 0) {
            body.append("⛔ STATUS: OUT OF STOCK - CRITICAL!\n");
            body.append("This product is completely out of stock and unavailable for purchase.\n\n");
        } else if (newStock <= 5) {
            body.append("⚠️ STATUS: LOW STOCK - ACTION REQUIRED!\n");
            body.append("This product is running critically low and needs immediate restocking.\n\n");
        }
        
        body.append("SELLER INFORMATION:\n");
        body.append("-------------------\n");
        body.append("Seller Name: ").append(product.getSeller().getName()).append("\n");
        body.append("Seller Email: ").append(product.getSeller().getEmail()).append("\n");
        if (product.getSeller().getPhone() != null) {
            body.append("Seller Phone: ").append(product.getSeller().getPhone()).append("\n");
        }
        body.append("\n");
        
        body.append("RECOMMENDED ACTIONS:\n");
        body.append("-------------------\n");
        body.append("1. Contact the seller to arrange restocking\n");
        body.append("2. Check recent sales trends for this product\n");
        body.append("3. Consider placing a bulk order if demand is high\n");
        body.append("4. Update product visibility if stock is critically low\n\n");
        
        body.append("Alert Time: ").append(timestamp).append("\n");
        body.append("Alert Trigger: Stock quantity fell to or below minimum threshold (5 units)\n\n");
        
        body.append("---\n");
        body.append("This is an automated message from Musical Instrument System.\n");
        body.append("Please do not reply to this email.\n");
        body.append("For questions, contact: support@musicstore.com\n");

        return body.toString();
    }

    public void sendOutOfStockAlert(Product product) {
        try {
            System.out.println("=== SENDING OUT OF STOCK ALERT EMAIL ===");
            System.out.println("Product: " + product.getName());
            System.out.println("Admin Email: " + adminEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(adminEmail);
            message.setSubject("⛔ OUT OF STOCK - " + product.getName());
            
            String emailBody = buildOutOfStockEmailBody(product);
            message.setText(emailBody);

            mailSender.send(message);
            System.out.println("✅ Out of stock alert email sent successfully");

        } catch (Exception e) {
            System.err.println("❌ Failed to send out of stock alert email: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private String buildOutOfStockEmailBody(Product product) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        String timestamp = LocalDateTime.now().format(formatter);

        StringBuilder body = new StringBuilder();
        body.append("⛔ CRITICAL: PRODUCT OUT OF STOCK\n");
        body.append("=====================================\n\n");
        
        body.append("Dear Administrator,\n\n");
        body.append("URGENT: The following product is now completely out of stock.\n\n");
        
        body.append("Product: ").append(product.getName()).append("\n");
        body.append("Category: ").append(product.getCategory()).append("\n");
        body.append("Seller: ").append(product.getSeller().getName()).append("\n");
        body.append("Price: $").append(String.format("%.2f", product.getPrice())).append("\n\n");
        
        body.append("IMMEDIATE ACTION REQUIRED:\n");
        body.append("- Contact seller for emergency restocking\n");
        body.append("- Update product visibility to prevent orders\n");
        body.append("- Check if there are pending orders for this product\n\n");
        
        body.append("Alert Time: ").append(timestamp).append("\n\n");
        body.append("---\n");
        body.append("Musical Instrument System - Automated Alert\n");

        return body.toString();
    }
}
