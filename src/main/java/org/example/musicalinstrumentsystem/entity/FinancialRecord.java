package org.example.musicalinstrumentsystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "financial_records")
public class FinancialRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String type; // "INCOME" or "EXPENSE"

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FinancialRecord() {
        this.type = "INCOME"; // default
    }

    public FinancialRecord(String title, String description, Double amount) {
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.type = "INCOME"; // default
        this.createdAt = LocalDateTime.now();
    }

    public FinancialRecord(String title, String description, Double amount, String type) {
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.type = type != null ? type.toUpperCase() : "INCOME";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type != null ? type.toUpperCase() : null; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt =createdAt;}
}
