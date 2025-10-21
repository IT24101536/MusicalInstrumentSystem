package org.example.musicalinstrumentsystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_reports")
public class FinancialReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String reportName;

    @Column(nullable = false)
    private String reportPeriod;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Double totalRevenue;

    @Column(nullable = false)
    private Double pendingRevenue;

    @Column(nullable = false)
    private Long totalTransactions;

    @Column(nullable = false)
    private Double averageOrderValue;

    @Column(nullable = false)
    private Double growthRate;

    @Column(nullable = false)
    private Integer ordersInRange;

    @Column(columnDefinition = "TEXT")
    private String salesByCategoryJson;

    @Column(columnDefinition = "TEXT")
    private String topProductsJson;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public FinancialReport() {
        this.createdAt = LocalDateTime.now();
    }

    public FinancialReport(String reportName, String reportPeriod, LocalDateTime startDate, LocalDateTime endDate) {
        this();
        this.reportName = reportName;
        this.reportPeriod = reportPeriod;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getReportPeriod() {
        return reportPeriod;
    }

    public void setReportPeriod(String reportPeriod) {
        this.reportPeriod = reportPeriod;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(Double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Double getPendingRevenue() {
        return pendingRevenue;
    }

    public void setPendingRevenue(Double pendingRevenue) {
        this.pendingRevenue = pendingRevenue;
    }

    public Long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(Long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public Double getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(Double averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public Double getGrowthRate() {
        return growthRate;
    }

    public void setGrowthRate(Double growthRate) {
        this.growthRate = growthRate;
    }

    public Integer getOrdersInRange() {
        return ordersInRange;
    }

    public void setOrdersInRange(Integer ordersInRange) {
        this.ordersInRange = ordersInRange;
    }

    public String getSalesByCategoryJson() {
        return salesByCategoryJson;
    }

    public void setSalesByCategoryJson(String salesByCategoryJson) {
        this.salesByCategoryJson = salesByCategoryJson;
    }

    public String getTopProductsJson() {
        return topProductsJson;
    }

    public void setTopProductsJson(String topProductsJson) {
        this.topProductsJson = topProductsJson;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
}
}
