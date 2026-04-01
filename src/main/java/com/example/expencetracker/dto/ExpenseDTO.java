package com.example.expencetracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
 
/**
 * Data Transfer Objects for Expense.
 *
 * Why DTOs instead of exposing the domain model directly?
 *   1. The API contract stays stable even when the DB schema changes.
 *   2. We control exactly what fields the client sends / receives.
 *   3. Validation lives here, not in the domain model.
 *
 * Inner static classes group Request and Response in one file
 * without polluting the package with tiny separate files.
 */
public class ExpenseDTO {
 
    // ─────────────────────────────────────────────────────
    // REQUEST — what the client sends on POST / PUT
    // ─────────────────────────────────────────────────────
 
    /**
     * Payload the client sends when creating or updating an expense.
     * Spring's @Valid annotation on the controller triggers these constraints.
     */
    public static class Request {
 
        @NotBlank(message = "Title is required")
        private String title;
 
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
        private BigDecimal amount;
 
        @NotBlank(message = "Category is required")
        private String category;
 
        @NotNull(message = "Date is required")
        @PastOrPresent(message = "Expense date cannot be in the future")
        private LocalDate date;
 
        private String description;

		// Constructors
		public Request() {
		}

		public Request(String title, BigDecimal amount, String category, LocalDate date, String description) {
			this.title = title;
			this.amount = amount;
			this.category = category;
			this.date = date;
			this.description = description;
		}

		// Getters and Setters
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
    }
 
    // ─────────────────────────────────────────────────────
    // RESPONSE — what we send back to the client
    // ─────────────────────────────────────────────────────
 
    /**
     * Read-only view of an expense returned by GET / POST / PUT responses.
     * Includes audit timestamps so the client can display "created" / "modified" times.
     */
    public static class Response {
 
        private Long id;
        private String title;
        private BigDecimal amount;
        private String category;
        private LocalDate date;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

		// Constructors
		public Response() {
		}

		public Response(Long id, String title, BigDecimal amount, String category, LocalDate date, String description,
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
}
