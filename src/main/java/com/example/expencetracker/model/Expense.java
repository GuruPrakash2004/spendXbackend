package com.example.expencetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
 
/**
 * Domain model — mirrors the `expenses` table row for row.
 *
 * We use BigDecimal for amount (not double/float) because floating-point
 * arithmetic is imprecise for money. BigDecimal gives exact decimal math.
 */
public class Expense {
 
    /** Primary key — auto-incremented by the database. */
    private Long id;
 
    /** Short label for the expense e.g. "Team lunch", "AWS bill". */
    private String title;
 
    /** Monetary value of the expense. */
    private BigDecimal amount;
 
    /** Logical group the expense belongs to. */
    private String category;
 
    /** Calendar date the expense actually occurred. */
    private LocalDate date;
 
    /** Optional notes or context about the expense. */
    private String description;
 
    /** Audit timestamps — set automatically by the DB. */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

	// Constructors
	public Expense() {
	}

	public Expense(Long id, String title, BigDecimal amount, String category, LocalDate date, String description,
			LocalDateTime createdAt, LocalDateTime updatedAt) {
		this.id = id;
		this.title = title;
		this.amount = amount;
		this.category = category;
		this.date = date;
		this.description = description;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}