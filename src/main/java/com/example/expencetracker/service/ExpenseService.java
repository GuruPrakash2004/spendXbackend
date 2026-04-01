package com.example.expencetracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.expencetracker.dto.ExpenseDTO;
import com.example.expencetracker.exception.ResourceNotFoundException;
import com.example.expencetracker.mapper.ExpenseMapper;
import com.example.expencetracker.model.Expense;
import com.example.expencetracker.repository.ExpenseRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
 
/**
 * Business logic layer for Expense operations.
 *
 * Responsibilities:
 *   1. Orchestrate DB reads/writes via ExpenseRepository.
 *   2. Convert between domain models and DTOs via ExpenseMapper.
 *   3. Publish domain events to ActiveMQ after each write operation,
 *      so Elasticsearch can index the change asynchronously.
 *   4. Enforce business rules (e.g. existence checks before update/delete).
 *
 * The service layer is intentionally kept free of HTTP concerns —
 * it knows nothing about HttpServletRequest, status codes, or JSON.
 * That responsibility belongs solely to the controller.
 */

@Service

public class ExpenseService {
 
    private final ExpenseRepository expenseRepository;
    private final ExpenseMapper      expenseMapper;
    private final JmsTemplate        jmsTemplate;
    
    
    public ExpenseService(ExpenseRepository expenseRepository, ExpenseMapper expenseMapper, JmsTemplate jmsTemplate) {
    		
    			this.expenseRepository = expenseRepository;
    			this.expenseMapper = expenseMapper;
    			this.jmsTemplate = jmsTemplate;
    }
    
    
    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);
 
    // Queue names are injected from application.yml — no hardcoded strings in Java
    @Value("${app.queue.expense-created}") private String createdQueue;
    @Value("${app.queue.expense-updated}") private String updatedQueue;
    @Value("${app.queue.expense-deleted}") private String deletedQueue;
 
 
    // ─────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────
 
    /**
     * Persists a new expense and fires a "created" event to the MQ.
     *
     * @Transactional ensures that if the DB insert fails mid-way,
     * the entire operation rolls back — no partial writes.
     */
    @Transactional
    public ExpenseDTO.Response create(ExpenseDTO.Request request) {
        log.info("Creating expense: title={}, amount={}", request.getTitle(), request.getAmount());
 
        // Map the incoming DTO to a domain model
        Expense expense = expenseMapper.toModel(request);
 
        // Set audit timestamps — the DB could do this too, but doing it here
        // keeps the logic explicit and testable without a live DB.
        expense.setCreatedAt(LocalDateTime.now());
        expense.setUpdatedAt(LocalDateTime.now());
 
        // Persist to PostgreSQL
        Expense saved = expenseRepository.save(expense);
 
        // Publish to MQ so the ES listener can index this new expense asynchronously.
        // Using convertAndSend serializes the object to JSON automatically.
        publishEvent(createdQueue, saved);
 
        log.info("Expense created successfully with id={}", saved.getId());
        return expenseMapper.toResponse(saved);
    }
 
 
    // ─────────────────────────────────────────────────────
    // READ — list all
    // ─────────────────────────────────────────────────────
 
    /**
     * Retrieves all expenses from the DB and converts them to response DTOs.
     * Stream + map is used instead of a for-loop for a cleaner, functional style.
     */
    public List<ExpenseDTO.Response> findAll() {
        log.debug("Fetching all expenses");
        return expenseRepository.findAll()
                .stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
    }
 
 
    // ─────────────────────────────────────────────────────
    // READ — single by ID
    // ─────────────────────────────────────────────────────
 
    /**
     * Looks up a single expense by its ID.
     *
     * orElseThrow: if the Optional returned by the repository is empty,
     * we immediately throw ResourceNotFoundException which the global
     * exception handler converts to a 404 response.
     */
    public ExpenseDTO.Response findById(Long id) {
        log.debug("Fetching expense id={}", id);
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
        return expenseMapper.toResponse(expense);
    }
 
 
    // ─────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────
 
    /**
     * Updates an existing expense and fires an "updated" event.
     *
     * Pattern:
     *   1. Fetch the current state from DB (throws 404 if missing).
     *   2. Apply the request fields onto the existing model in-place.
     *   3. Refresh the updatedAt timestamp.
     *   4. Persist and publish.
     */
    @Transactional
    public ExpenseDTO.Response update(Long id, ExpenseDTO.Request request) {
        log.info("Updating expense id={}", id);
 
        // Verify the record exists — throws 404 if not
        Expense existing = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
 
        // MapStruct copies only the request fields onto the existing model,
        // preserving id and createdAt which the client should not overwrite.
        expenseMapper.updateModel(request, existing);
        existing.setUpdatedAt(LocalDateTime.now());
 
        Expense updated = expenseRepository.update(existing);
        publishEvent(updatedQueue, updated);
 
        log.info("Expense updated successfully id={}", id);
        return expenseMapper.toResponse(updated);
    }
 
 
    // ─────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────
 
    /**
     * Deletes an expense and fires a "deleted" event carrying just the ID.
     * The Elasticsearch listener will remove the document for this ID from the index.
     */
    @Transactional
    public void delete(Long id) {
        log.info("Deleting expense id={}", id);
 
        // Guard: ensure the expense actually exists before we try to delete it
        expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id: " + id));
 
        expenseRepository.deleteById(id);
 
        // Send just the ID string — the listener only needs it to remove the ES document
        jmsTemplate.convertAndSend(deletedQueue, id.toString());
 
        log.info("Expense deleted id={}", id);
    }
 
 
    // ─────────────────────────────────────────────────────
    // FILTER
    // ─────────────────────────────────────────────────────
 
    /**
     * Returns all expenses for a given category.
     * Useful for the analytics dashboard (pie chart / bar chart per category).
     */
    public List<ExpenseDTO.Response> findByCategory(String category) {
        log.debug("Fetching expenses for category={}", category);
        return expenseRepository.findByCategory(category)
                .stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
    }
 
 
    // ─────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────────────
 
    /**
     * Sends an Expense domain object to the specified JMS queue.
     * JmsTemplate serializes it to JSON automatically using the
     * MessageConverter configured in JmsConfig.
     *
     * Failures here are logged but do NOT roll back the DB transaction —
     * the primary write must succeed regardless of messaging hiccups.
     */
    private void publishEvent(String queue, Expense expense) {
        try {
            jmsTemplate.convertAndSend(queue, expense);
            log.debug("Published event to queue={} for expenseId={}", queue, expense.getId());
        } catch (Exception e) {
            // Log and swallow — do not let a messaging failure undo a successful DB write
            log.error("Failed to publish event to queue={}: {}", queue, e.getMessage(), e);
        }
    }
}
