package org.example.musicalinstrumentsystem.service;

import org.example.musicalinstrumentsystem.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

@Service
@SessionScope
public class SessionService {
    private User currentUser;
    private String userRole;

    public void login(User user) {
        this.currentUser = user;
        this.userRole = user.getRole();
        System.out.println(" Session created for: " + user.getEmail() + " as " + user.getRole());
    }

    public void logout() {
        System.out.println(" Session ended for: " + (currentUser != null ? currentUser.getEmail() : "Unknown"));
        this.currentUser = null;
        this.userRole = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getUserRole() {
        return userRole;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(userRole);
    }

    public boolean isSeller() {
        return "SELLER".equals(userRole);
    }

    public boolean isBuyer() {
        return "BUYER".equals(userRole);
    }

    public boolean isStockManager() {
        return "STOCK_MANAGER".equals(userRole);
    }

    public Long getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    public String getCurrentUserEmail() {
        return currentUser != null ? currentUser.getEmail() : null;
    }

    public String getCurrentUserName() {
        return currentUser != null ? currentUser.getName() : null;
    }
}