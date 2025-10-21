package org.example.musicalinstrumentsystem.controller;

import org.example.musicalinstrumentsystem.entity.FinancialReport;
import org.example.musicalinstrumentsystem.entity.Order;
import org.example.musicalinstrumentsystem.entity.User;
import org.example.musicalinstrumentsystem.entity.ReportComment;
import org.example.musicalinstrumentsystem.entity.ReportSchedule;
import org.example.musicalinstrumentsystem.entity.FinancialRecord;
import org.example.musicalinstrumentsystem.service.FinancialReportService;
import org.example.musicalinstrumentsystem.service.OrderService;
import org.example.musicalinstrumentsystem.service.ProductService;
import org.example.musicalinstrumentsystem.service.SessionService;
import org.example.musicalinstrumentsystem.service.UserService;
import org.example.musicalinstrumentsystem.service.ReportCommentService;
import org.example.musicalinstrumentsystem.service.ReportScheduleService;
import org.example.musicalinstrumentsystem.service.FinancialRecordService;
import org.example.musicalinstrumentsystem.service.PaymentRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private FinancialReportService financialReportService;

    @Autowired
    private FinancialRecordService financialRecordService;

    @Autowired
    private ReportCommentService reportCommentService;

    @Autowired
    private ReportScheduleService reportScheduleService;

    @Autowired
    private PaymentRecordService paymentRecordService;

    //  ADMIN DASHBOARD
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        System.out.println("=== ACCESSING ADMIN DASHBOARD ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();
        model.addAttribute("user", currentUser);

        // Get system statistics
        long totalUsers = userService.getAllUsers().size();
        long totalProducts = productService.getAllProducts().size();
        long totalOrders = orderService.getAllOrders().size();
        
        // Get pending orders count from order statistics
        Map<String, Object> orderStats = orderService.getOrderStatistics();
        long pendingOrders = (Long) orderStats.getOrDefault("pendingOrders", 0L);

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);

        return "admin/dashboard";
    }

    //  FINANCIAL REPORTS
    @GetMapping("/reports")
    public String viewFinancialReports(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        System.out.println("=== ACCESSING FINANCIAL REPORTS ===");
        System.out.println("Period: " + period);
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();
        model.addAttribute("user", currentUser);

        // Determine date range
        LocalDateTime start;
        LocalDateTime end = LocalDateTime.now();

        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            // Custom date range
            start = LocalDateTime.parse(startDate + "T00:00:00");
            end = LocalDateTime.parse(endDate + "T23:59:59");
            model.addAttribute("selectedPeriod", "custom");
        } else if ("month".equals(period)) {
            start = LocalDateTime.now().minusMonths(1);
            model.addAttribute("selectedPeriod", "month");
        } else if ("quarter".equals(period)) {
            start = LocalDateTime.now().minusMonths(3);
            model.addAttribute("selectedPeriod", "quarter");
        } else if ("year".equals(period)) {
            start = LocalDateTime.now().minusYears(1);
            model.addAttribute("selectedPeriod", "year");
        } else {
            // Default to current month
            start = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            model.addAttribute("selectedPeriod", "current");
        }


        final LocalDateTime finalStart = start;
        final LocalDateTime finalEnd = end;

        // Get all completed orders in date range
        List<Order> allOrders = orderService.getAllOrders();
        List<Order> ordersInRange = allOrders.stream()
                .filter(order -> order.getOrderDate() != null
                        && !order.getOrderDate().isBefore(finalStart)
                        && !order.getOrderDate().isAfter(finalEnd))
                .collect(Collectors.toList());

        // Calculate financial metrics
        double totalRevenue = ordersInRange.stream()
                .filter(order -> "COMPLETED".equals(order.getPaymentStatus()) || "DELIVERED".equals(order.getStatus()))
                .mapToDouble(Order::getTotalAmount)
                .sum();

        double pendingRevenue = ordersInRange.stream()
                .filter(order -> "PENDING".equals(order.getPaymentStatus()))
                .mapToDouble(Order::getTotalAmount)
                .sum();

        long totalTransactions = ordersInRange.stream()
                .filter(order -> "COMPLETED".equals(order.getPaymentStatus()))
                .count();

        double averageOrderValue = totalTransactions > 0 ? totalRevenue / totalTransactions : 0;

        // Calculate growth compared to previous period
        long periodDays = java.time.Duration.between(finalStart, finalEnd).toDays();
        LocalDateTime previousStart = finalStart.minusDays(periodDays);
        LocalDateTime previousEnd = finalStart;

        List<Order> previousOrders = allOrders.stream()
                .filter(order -> order.getOrderDate() != null
                        && !order.getOrderDate().isBefore(previousStart)
                        && order.getOrderDate().isBefore(previousEnd))
                .collect(Collectors.toList());

        double previousRevenue = previousOrders.stream()
                .filter(order -> "COMPLETED".equals(order.getPaymentStatus()))
                .mapToDouble(Order::getTotalAmount)
                .sum();

        double growthRate = previousRevenue > 0 ?
                ((totalRevenue - previousRevenue) / previousRevenue) * 100 : 0;

        // Sales by category
        Map<String, Double> salesByCategory = new HashMap<>();
        Map<String, Integer> quantityByCategory = new HashMap<>();

        ordersInRange.stream()
                .filter(order -> "COMPLETED".equals(order.getPaymentStatus()))
                .forEach(order -> {
                    if (order.getOrderItems() != null) {
                        order.getOrderItems().forEach(item -> {
                            if (item.getProduct() != null) {
                                String category = item.getProduct().getCategory();
                                salesByCategory.merge(category, item.getTotalPrice(), Double::sum);
                                quantityByCategory.merge(category, item.getQuantity(), Integer::sum);
                            }
                        });
                    }
                });

        // Top products by revenue
        Map<String, Double> productRevenue = new HashMap<>();
        ordersInRange.stream()
                .filter(order -> "COMPLETED".equals(order.getPaymentStatus()))
                .forEach(order -> {
                    if (order.getOrderItems() != null) {
                        order.getOrderItems().forEach(item -> {
                            if (item.getProduct() != null) {
                                String productName = item.getProduct().getName();
                                productRevenue.merge(productName, item.getTotalPrice(), Double::sum);
                            }
                        });
                    }
                });

        List<Map.Entry<String, Double>> topProducts = productRevenue.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        // Monthly sales trend (for charts)
        Map<String, Double> monthlySales = new TreeMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");

        ordersInRange.stream()
                .filter(order -> "COMPLETED".equals(order.getPaymentStatus()))
                .forEach(order -> {
                    String monthKey = order.getOrderDate().format(monthFormatter);
                    monthlySales.merge(monthKey, order.getTotalAmount(), Double::sum);
                });

        // Daily sales for last 30 days
        Map<String, Double> dailySales = new TreeMap<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("MMM dd");

        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);
        allOrders.stream()
                .filter(order -> order.getOrderDate() != null
                        && order.getOrderDate().isAfter(last30Days)
                        && "COMPLETED".equals(order.getPaymentStatus()))
                .forEach(order -> {
                    String dayKey = order.getOrderDate().format(dayFormatter);
                    dailySales.merge(dayKey, order.getTotalAmount(), Double::sum);
                });

        // Add all data to model
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("pendingRevenue", pendingRevenue);
        model.addAttribute("totalTransactions", totalTransactions);
        model.addAttribute("averageOrderValue", averageOrderValue);
        model.addAttribute("growthRate", growthRate);
        model.addAttribute("salesByCategory", salesByCategory);
        model.addAttribute("quantityByCategory", quantityByCategory);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("monthlySales", monthlySales);
        model.addAttribute("dailySales", dailySales);
        model.addAttribute("startDate", start.format(DateTimeFormatter.ISO_LOCAL_DATE));
        model.addAttribute("endDate", end.format(DateTimeFormatter.ISO_LOCAL_DATE));
        model.addAttribute("ordersInRange", ordersInRange.size());

        return "admin/financial-reports";
    }

    //FINANCIAL REPORTS CRUD OPERATIONS

    //  Save a financial report
    @PostMapping("/reports/save")
    public String saveFinancialReport(
            @RequestParam String reportName,
            @RequestParam String reportPeriod,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam Double totalRevenue,
            @RequestParam Double pendingRevenue,
            @RequestParam Long totalTransactions,
            @RequestParam Double averageOrderValue,
            @RequestParam Double growthRate,
            @RequestParam Integer ordersInRange,
            @RequestParam(required = false) String salesByCategoryJson,
            @RequestParam(required = false) String topProductsJson,
            @RequestParam(required = false) String notes) {

        System.out.println("=== SAVING FINANCIAL REPORT ===");
        System.out.println("Report Name: " + reportName);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            User currentUser = sessionService.getCurrentUser();
            LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");


            Map<String, Double> salesByCategory = new HashMap<>();
            List<Map.Entry<String, Double>> topProducts = new ArrayList<>();

            financialReportService.saveReport(
                    reportName,
                    reportPeriod,
                    start,
                    end,
                    totalRevenue,
                    pendingRevenue,
                    totalTransactions,
                    averageOrderValue,
                    growthRate,
                    ordersInRange,
                    salesByCategory,
                    topProducts,
                    notes,
                    currentUser
            );

            System.out.println("✅ Report saved successfully");
            return "redirect:/admin/reports/saved?success=report_saved";

        } catch (Exception e) {
            System.out.println("❌ Error saving report: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/reports?error=save_failed";
        }
    }

    //  View all saved reports
    @GetMapping("/reports/saved")
    public String viewSavedReports(Model model) {
        System.out.println("=== VIEWING SAVED FINANCIAL REPORTS ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();
        List<FinancialReport> savedReports = financialReportService.getAllReports();

        model.addAttribute("user", currentUser);
        model.addAttribute("savedReports", savedReports);
        model.addAttribute("totalSavedReports", savedReports.size());

        return "admin/saved-reports";
    }

    //  View a specific saved report
    @GetMapping("/reports/view/{id}")
    public String viewSavedReport(@PathVariable Long id, Model model) {
        System.out.println("=== VIEWING SAVED REPORT ID: " + id + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        Optional<FinancialReport> reportOpt = financialReportService.getReportById(id);

        if (reportOpt.isEmpty()) {
            return "redirect:/admin/reports/saved?error=report_not_found";
        }

        FinancialReport report = reportOpt.get();
        User currentUser = sessionService.getCurrentUser();

        model.addAttribute("user", currentUser);
        model.addAttribute("report", report);

        return "admin/view-saved-report";
    }

    //  Update report name and notes
    @PostMapping("/reports/update/{id}")
    public String updateFinancialReport(
            @PathVariable Long id,
            @RequestParam String reportName,
            @RequestParam(required = false) String notes) {

        System.out.println("=== UPDATING REPORT ID: " + id + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            financialReportService.updateReport(id, reportName, notes);
            return "redirect:/admin/reports/view/" + id + "?success=report_updated";
        } catch (Exception e) {
            System.out.println("❌ Error updating report: " + e.getMessage());
            return "redirect:/admin/reports/view/" + id + "?error=update_failed";
        }
    }

    //  Delete a saved report
    @GetMapping("/reports/delete/{id}")
    public String deleteFinancialReport(@PathVariable Long id) {
        System.out.println("=== DELETING REPORT ID: " + id + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            boolean deleted = financialReportService.deleteReport(id);
            if (deleted) {
                return "redirect:/admin/reports/saved?success=report_deleted";
            } else {
                return "redirect:/admin/reports/saved?error=report_not_found";
            }
        } catch (Exception e) {
            System.out.println("❌ Error deleting report: " + e.getMessage());
            return "redirect:/admin/reports/saved?error=delete_failed";
        }
    }

    // USER MANAGEMENT
    @GetMapping("/users")
    public String manageUsers(Model model) {
        System.out.println("=== MANAGING USERS ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        List<User> allUsers = userService.getAllUsers();
        
        // Calculate user counts by role
        long totalBuyers = userService.countUsersByRole("BUYER");
        long totalSellers = userService.countUsersByRole("SELLER");
        long totalStockManagers = userService.countUsersByRole("STOCK_MANAGER");
        long totalAdmins = userService.countUsersByRole("ADMIN");

        System.out.println("👥 User Statistics:");
        System.out.println("   - Buyers: " + totalBuyers);
        System.out.println("   - Sellers: " + totalSellers);
        System.out.println("   - Stock Managers: " + totalStockManagers);
        System.out.println("   - Admins: " + totalAdmins);

        model.addAttribute("users", allUsers);
        model.addAttribute("user", sessionService.getCurrentUser());
        model.addAttribute("totalBuyers", totalBuyers);
        model.addAttribute("totalSellers", totalSellers);
        model.addAttribute("totalStockManagers", totalStockManagers);
        model.addAttribute("totalAdmins", totalAdmins);

        return "admin/users";
    }

    //  ORDER MANAGEMENT
    @GetMapping("/orders")
    public String manageOrders(Model model) {
        System.out.println("=== MANAGING ORDERS ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        List<Order> allOrders = orderService.getAllOrders();
        
        // Calculate order statistics by status
        long pendingOrders = allOrders.stream().filter(o -> "PENDING".equals(o.getStatus())).count();
        long confirmedOrders = allOrders.stream().filter(o -> "CONFIRMED".equals(o.getStatus())).count();
        long shippedOrders = allOrders.stream().filter(o -> "SHIPPED".equals(o.getStatus())).count();
        long deliveredOrders = allOrders.stream().filter(o -> "DELIVERED".equals(o.getStatus())).count();
        long cancelledOrders = allOrders.stream().filter(o -> "CANCELLED".equals(o.getStatus())).count();

        System.out.println("📦 Order Statistics:");
        System.out.println("   - Pending: " + pendingOrders);
        System.out.println("   - Confirmed: " + confirmedOrders);
        System.out.println("   - Shipped: " + shippedOrders);
        System.out.println("   - Delivered: " + deliveredOrders);
        System.out.println("   - Cancelled: " + cancelledOrders);

        model.addAttribute("orders", allOrders);
        model.addAttribute("user", sessionService.getCurrentUser());
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("confirmedOrders", confirmedOrders);
        model.addAttribute("shippedOrders", shippedOrders);
        model.addAttribute("deliveredOrders", deliveredOrders);
        model.addAttribute("cancelledOrders", cancelledOrders);

        return "admin/orders";
    }

    //  Update order delivery status
    @PostMapping("/orders/update-status/{id}")
    public String updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        System.out.println("=== UPDATING ORDER STATUS ===");
        System.out.println("Order ID: " + id);
        System.out.println("New Status: " + status);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            Optional<Order> orderOpt = orderService.getOrderById(id);
            if (orderOpt.isEmpty()) {
                return "redirect:/admin/orders?error=order_not_found";
            }

            Order order = orderOpt.get();
            String oldStatus = order.getStatus();
            order.setStatus(status.toUpperCase());

            // If status is DELIVERED, set delivery date
            if ("DELIVERED".equals(status.toUpperCase()) && order.getDeliveryDate() == null) {
                order.setDeliveryDate(LocalDateTime.now());
            }

            orderService.updateOrder(order);
            System.out.println("✅ Order status updated: " + oldStatus + " -> " + status);

            return "redirect:/admin/orders?success=status_updated";
        } catch (Exception e) {
            System.out.println("❌ Error updating order status: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/orders?error=update_failed";
        }
    }

    //  View order details
    @GetMapping("/orders/view/{id}")
    public String viewOrderDetails(@PathVariable Long id, Model model) {
        System.out.println("=== VIEWING ORDER DETAILS: " + id + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        Optional<Order> orderOpt = orderService.getOrderById(id);
        if (orderOpt.isEmpty()) {
            return "redirect:/admin/orders?error=order_not_found";
        }

        model.addAttribute("order", orderOpt.get());
        model.addAttribute("user", sessionService.getCurrentUser());

        return "admin/order-details";
    }

    //  Delete completed, confirmed, or cancelled orders
    @GetMapping("/orders/delete/{id}")
    public String deleteOrder(@PathVariable Long id) {
        System.out.println("=== DELETING ORDER: " + id + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            Optional<Order> orderOpt = orderService.getOrderById(id);
            if (orderOpt.isEmpty()) {
                System.out.println("❌ Order not found: " + id);
                return "redirect:/admin/orders?error=order_not_found";
            }

            Order order = orderOpt.get();
            String orderStatus = order.getStatus();
            double orderAmount = order.getTotalAmount();


            if (!"CONFIRMED".equals(orderStatus) && !"DELIVERED".equals(orderStatus) && !"CANCELLED".equals(orderStatus)) {
                System.out.println("❌ Cannot delete order with status: " + orderStatus);
                System.out.println("⚠️ Only CONFIRMED, DELIVERED, or CANCELLED orders can be deleted");
                return "redirect:/admin/orders?error=cannot_delete_active";
            }

            // Delete the order
            boolean deleted = orderService.deleteOrder(id);
            
            if (deleted) {
                System.out.println("✅ Order deleted successfully: " + id);
                System.out.println("   Order Status: " + orderStatus);
                System.out.println("   Order Amount: $" + orderAmount);
                return "redirect:/admin/orders?success=order_deleted";
            } else {
                System.out.println("❌ Failed to delete order: " + id);
                return "redirect:/admin/orders?error=delete_failed";
            }

        } catch (Exception e) {
            System.out.println("❌ Error deleting order: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/orders?error=delete_failed";
        }
    }

    //  Show create user form
    @GetMapping("/users/new")
    public String showCreateUserForm(Model model) {
        System.out.println("=== SHOWING CREATE USER FORM ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        model.addAttribute("user", sessionService.getCurrentUser());
        model.addAttribute("newUser", new User());

        return "admin/new-user";
    }

    //  Process create user form
    @PostMapping("/users/create")
    public String createUser(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String role,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address) {

        System.out.println("=== CREATING NEW USER ===");
        System.out.println("Name: " + name);
        System.out.println("Email: " + email);
        System.out.println("Role: " + role);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            // Check if email already exists
            if (userService.getUserByEmail(email).isPresent()) {
                return "redirect:/admin/users/new?error=email_exists";
            }

            User newUser = new User();
            newUser.setName(name);
            newUser.setEmail(email);
            newUser.setPassword(password);
            newUser.setRole(role.toUpperCase());
            newUser.setPhone(phone);
            newUser.setAddress(address);

            userService.createUser(newUser);
            System.out.println("✅ User created successfully");

            return "redirect:/admin/users?success=user_created";
        } catch (Exception e) {
            System.out.println("❌ Error creating user: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/users/new?error=create_failed";
        }
    }

    //  Show edit user form
    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        System.out.println("=== SHOWING EDIT USER FORM FOR ID: " + id + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        Optional<User> userToEditOpt = userService.getUserById(id);
        if (userToEditOpt.isEmpty()) {
            return "redirect:/admin/users?error=user_not_found";
        }

        model.addAttribute("user", sessionService.getCurrentUser());
        model.addAttribute("userToEdit", userToEditOpt.get());

        return "admin/edit-user";
    }

    //  Process edit user form
    @PostMapping("/users/update")
    public String updateUser(
            @RequestParam Long id,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String role,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address) {

        System.out.println("=== UPDATING USER ID: " + id + " ===");
        System.out.println("New Name: " + name);
        System.out.println("New Email: " + email);
        System.out.println("New Role: " + role);

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            Optional<User> existingUserOpt = userService.getUserById(id);
            if (existingUserOpt.isEmpty()) {
                return "redirect:/admin/users?error=user_not_found";
            }
            
            User existingUser = existingUserOpt.get();

            // Check if email is taken by another user
            Optional<User> userWithSameEmailOpt = userService.getUserByEmail(email);
            if (userWithSameEmailOpt.isPresent() && !userWithSameEmailOpt.get().getId().equals(id)) {
                return "redirect:/admin/users/edit/" + id + "?error=email_exists";
            }

            existingUser.setName(name);
            existingUser.setEmail(email);
            existingUser.setRole(role.toUpperCase());
            existingUser.setPhone(phone);
            existingUser.setAddress(address);

            userService.updateUser(existingUser);
            System.out.println("✅ User updated successfully");

            return "redirect:/admin/users?success=user_updated";
        } catch (Exception e) {
            System.out.println("❌ Error updating user: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/users/edit/" + id + "?error=update_failed";
        }
    }

    //  Delete a user
    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        System.out.println("=== DELETING USER ID: " + id + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            User currentUser = sessionService.getCurrentUser();
            
            // Prevent admin from deleting themselves
            if (currentUser.getId().equals(id)) {
                System.out.println("❌ Cannot delete yourself");
                return "redirect:/admin/users?error=cannot_delete_self";
            }

            Optional<User> userToDeleteOpt = userService.getUserById(id);
            if (userToDeleteOpt.isEmpty()) {
                return "redirect:/admin/users?error=user_not_found";
            }

            userService.deleteUser(id);
            System.out.println("✅ User deleted successfully");

            return "redirect:/admin/users?success=user_deleted";
        } catch (Exception e) {
            System.out.println("❌ Error deleting user: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/users?error=delete_failed";
        }
    }



    //  Add comment to a report
    @PostMapping("/reports/{reportId}/comments/add")
    public String addReportComment(
            @PathVariable Long reportId,
            @RequestParam String commentText,
            @RequestParam String commentType) {

        System.out.println("=== ADDING COMMENT TO REPORT: " + reportId + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            Optional<FinancialReport> reportOpt = financialReportService.getReportById(reportId);
            if (reportOpt.isEmpty()) {
                return "redirect:/admin/reports/saved?error=report_not_found";
            }

            User currentUser = sessionService.getCurrentUser();
            reportCommentService.createComment(reportOpt.get(), currentUser, commentText, commentType);

            return "redirect:/admin/reports/view/" + reportId + "?success=comment_added";
        } catch (Exception e) {
            System.out.println("❌ Error adding comment: " + e.getMessage());
            return "redirect:/admin/reports/view/" + reportId + "?error=comment_failed";
        }
    }

    //  View comments for a report
    @GetMapping("/reports/{reportId}/comments")
    @ResponseBody
    public List<ReportComment> getReportComments(@PathVariable Long reportId) {
        System.out.println("=== FETCHING COMMENTS FOR REPORT: " + reportId + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return List.of();
        }

        Optional<FinancialReport> reportOpt = financialReportService.getReportById(reportId);
        if (reportOpt.isEmpty()) {
            return List.of();
        }

        return reportCommentService.getCommentsByReport(reportOpt.get());
    }

    //  Update a comment
    @PostMapping("/reports/comments/update/{commentId}")
    public String updateReportComment(
            @PathVariable Long commentId,
            @RequestParam String commentText,
            @RequestParam Long reportId) {

        System.out.println("=== UPDATING COMMENT: " + commentId + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            reportCommentService.updateComment(commentId, commentText);
            return "redirect:/admin/reports/view/" + reportId + "?success=comment_updated";
        } catch (Exception e) {
            System.out.println("❌ Error updating comment: " + e.getMessage());
            return "redirect:/admin/reports/view/" + reportId + "?error=update_failed";
        }
    }

    //  Delete a comment
    @GetMapping("/reports/comments/delete/{commentId}")
    public String deleteReportComment(
            @PathVariable Long commentId,
            @RequestParam Long reportId) {

        System.out.println("=== DELETING COMMENT: " + commentId + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            boolean deleted = reportCommentService.deleteComment(commentId);
            if (deleted) {
                return "redirect:/admin/reports/view/" + reportId + "?success=comment_deleted";
            } else {
                return "redirect:/admin/reports/view/" + reportId + "?error=comment_not_found";
            }
        } catch (Exception e) {
            System.out.println("❌ Error deleting comment: " + e.getMessage());
            return "redirect:/admin/reports/view/" + reportId + "?error=delete_failed";
        }
    }

    //  REPORT SCHEDULES CRUD OPERATIONS

    //  Create a new schedule
    @PostMapping("/reports/schedules/create")
    public String createReportSchedule(
            @RequestParam String scheduleName,
            @RequestParam String reportType,
            @RequestParam String frequency,
            @RequestParam(required = false) Integer dayOfWeek,
            @RequestParam(required = false) Integer dayOfMonth,
            @RequestParam(required = false) String emailRecipients,
            @RequestParam(required = false) String description) {

        System.out.println("=== CREATING REPORT SCHEDULE ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            User currentUser = sessionService.getCurrentUser();
            reportScheduleService.createSchedule(
                    scheduleName, reportType, frequency, dayOfWeek,
                    dayOfMonth, emailRecipients, description, currentUser);

            return "redirect:/admin/reports/schedules?success=schedule_created";
        } catch (Exception e) {
            System.out.println("❌ Error creating schedule: " + e.getMessage());
            return "redirect:/admin/reports/schedules?error=create_failed";
        }
    }

    // READ - View all schedules
    @GetMapping("/reports/schedules")
    public String viewReportSchedules(Model model) {
        System.out.println("=== VIEWING REPORT SCHEDULES ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        User currentUser = sessionService.getCurrentUser();
        List<ReportSchedule> schedules = reportScheduleService.getAllSchedules();
        long activeSchedules = reportScheduleService.countActiveSchedules();

        model.addAttribute("user", currentUser);
        model.addAttribute("schedules", schedules);
        model.addAttribute("activeSchedules", activeSchedules);

        return "admin/report-schedules";
    }

    //  View a specific schedule
    @GetMapping("/reports/schedules/{id}")
    public String viewReportSchedule(@PathVariable Long id, Model model) {
        System.out.println("=== VIEWING SCHEDULE: " + id + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        Optional<ReportSchedule> scheduleOpt = reportScheduleService.getScheduleById(id);
        if (scheduleOpt.isEmpty()) {
            return "redirect:/admin/reports/schedules?error=schedule_not_found";
        }

        User currentUser = sessionService.getCurrentUser();
        model.addAttribute("user", currentUser);
        model.addAttribute("schedule", scheduleOpt.get());

        return "admin/view-schedule";
    }

    //  Update a schedule
    @PostMapping("/reports/schedules/update/{id}")
    public String updateReportSchedule(
            @PathVariable Long id,
            @RequestParam String scheduleName,
            @RequestParam String reportType,
            @RequestParam String frequency,
            @RequestParam(required = false) Integer dayOfWeek,
            @RequestParam(required = false) Integer dayOfMonth,
            @RequestParam(required = false) String emailRecipients,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean isActive) {

        System.out.println("=== UPDATING SCHEDULE: " + id + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            reportScheduleService.updateSchedule(
                    id, scheduleName, reportType, frequency, dayOfWeek,
                    dayOfMonth, emailRecipients, description, isActive);

            return "redirect:/admin/reports/schedules?success=schedule_updated";
        } catch (Exception e) {
            System.out.println("❌ Error updating schedule: " + e.getMessage());
            return "redirect:/admin/reports/schedules?error=update_failed";
        }
    }

    // Toggle schedule active status
    @GetMapping("/reports/schedules/toggle/{id}")
    public String toggleScheduleStatus(@PathVariable Long id) {
        System.out.println("=== TOGGLING SCHEDULE STATUS: " + id + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            reportScheduleService.toggleScheduleStatus(id);
            return "redirect:/admin/reports/schedules?success=status_toggled";
        } catch (Exception e) {
            System.out.println("❌ Error toggling schedule: " + e.getMessage());
            return "redirect:/admin/reports/schedules?error=toggle_failed";
        }
    }

    //  Delete a schedule
    @GetMapping("/reports/schedules/delete/{id}")
    public String deleteReportSchedule(@PathVariable Long id) {
        System.out.println("=== DELETING SCHEDULE: " + id + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            boolean deleted = reportScheduleService.deleteSchedule(id);
            if (deleted) {
                return "redirect:/admin/reports/schedules?success=schedule_deleted";
            } else {
                return "redirect:/admin/reports/schedules?error=schedule_not_found";
            }
        } catch (Exception e) {
            System.out.println("❌ Error deleting schedule: " + e.getMessage());
            return "redirect:/admin/reports/schedules?error=delete_failed";
        }
    }

    //  FINANCIAL RECORDS CRUD OPERATIONS

    // List all financial records
    @GetMapping("/financial-records")
    public String viewFinancialRecords(Model model) {
        System.out.println("=== VIEWING FINANCIAL RECORDS ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            User currentUser = sessionService.getCurrentUser();
            List<FinancialRecord> records = financialRecordService.getAllRecords();

            model.addAttribute("user", currentUser);
            model.addAttribute("records", records);

            return "admin/financial-records";
        } catch (Exception e) {
            System.out.println("❌ Error loading financial records: " + e.getMessage());
            e.printStackTrace();

            // Fallback: show the page with an empty list and an error message (avoid HTTP 500)
            model.addAttribute("user", sessionService.getCurrentUser());
            model.addAttribute("records", java.util.Collections.emptyList());
            model.addAttribute("errorMessage", "Failed to load financial records: " + e.getMessage());
            return "admin/financial-records";
        }
    }

    //  Add a new financial record
    @PostMapping("/financial-records/add")
    public String addFinancialRecord(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam Double amount,
            @RequestParam(required = false) String type) {

        System.out.println("=== ADDING FINANCIAL RECORD ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            FinancialRecord record = new FinancialRecord(title, description, amount, type);
            financialRecordService.saveRecord(record);
            return "redirect:/admin/financial-records?success=record_added";
        } catch (Exception e) {
            System.out.println("❌ Error adding record: " + e.getMessage());
            return "redirect:/admin/financial-records?error=add_failed";
        }
    }

    //  Edit a financial record
    @PostMapping("/financial-records/edit/{id}")
    public String editFinancialRecord(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam Double amount,
            @RequestParam(required = false) String type) {

        System.out.println("=== EDITING FINANCIAL RECORD ID: " + id + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            Optional<FinancialRecord> recOpt = financialRecordService.getRecordById(id);
            if (recOpt.isEmpty()) {
                return "redirect:/admin/financial-records?error=record_not_found";
            }
            FinancialRecord record = recOpt.get();
            record.setTitle(title);
            record.setDescription(description);
            record.setAmount(amount);
            record.setType(type != null ? type.toUpperCase() : record.getType());
            financialRecordService.saveRecord(record);
            return "redirect:/admin/financial-records?success=record_updated";
        } catch (Exception e) {
            System.out.println("❌ Error updating record: " + e.getMessage());
            return "redirect:/admin/financial-records?error=update_failed";
        }
    }

    // Delete a financial record
    @GetMapping("/financial-records/delete/{id}")
    public String deleteFinancialRecord(@PathVariable Long id) {
        System.out.println("=== DELETING FINANCIAL RECORD ID: " + id + " ===");

        if (!sessionService.isLoggedIn() || !"ADMIN".equals(sessionService.getUserRole())) {
            return "redirect:/login?error=access_denied";
        }

        try {
            boolean deleted = financialRecordService.deleteRecord(id);
            if (deleted) {
                return "redirect:/admin/financial-records?success=record_deleted";
            } else {
                return "redirect:/admin/financial-records?error=record_not_found";
            }
        } catch (Exception e) {
            System.out.println("❌ Error deleting record: " + e.getMessage());
            return "redirect:/admin/financial-records?error=delete_failed";
        }
    }
}
