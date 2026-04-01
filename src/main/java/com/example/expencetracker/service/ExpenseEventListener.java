package com.example.expencetracker.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.example.expencetracker.model.Expense;
 
/**
 * JMS listener that consumes messages from ActiveMQ Artemis queues
 * and keeps the Elasticsearch index in sync with the Postgres database.
 *
 * Why async via MQ instead of calling ElasticsearchService directly from ExpenseService?
 *   1. Decoupling: the primary write path (DB) completes immediately and returns to the
 *      client without waiting for ES, which can be slower under load.
 *   2. Resilience: if ES is temporarily down, messages queue up in Artemis and are
 *      delivered once ES recovers — no data loss, no manual re-sync needed.
 *   3. Separation of concerns: the service responsible for CRUD doesn't need to
 *      know anything about the search index implementation.
 *
 * Each listener method is invoked on a separate thread managed by the JMS container,
 * so heavy ES operations never block the HTTP request threads.
 */

@Component
public class ExpenseEventListener {
 
    private final ElasticsearchService elasticsearchService;
    
    public ExpenseEventListener(ElasticsearchService elasticsearchService) {
		this.elasticsearchService = elasticsearchService;
	}
    
    
    private static final Logger log = LoggerFactory.getLogger(ExpenseEventListener.class);
 
    // ─────────────────────────────────────────────────────
    // CREATED
    // ─────────────────────────────────────────────────────
 
    /**
     * Indexes a newly created expense into Elasticsearch.
     *
     * destination maps to app.queue.expense-created in application.yml.
     * Spring JMS deserializes the incoming TextMessage JSON → Expense
     * using the MessageConverter configured in JmsConfig.
     */
    @JmsListener(destination = "${app.queue.expense-created}")
    public void onExpenseCreated(Expense expense) {
        log.info("MQ event received [CREATED] expenseId={}", expense.getId());
        elasticsearchService.indexExpense(expense);
    }
 
 
    // ─────────────────────────────────────────────────────
    // UPDATED
    // ─────────────────────────────────────────────────────
 
    /**
     * Re-indexes an updated expense.
     * ES upsert semantics mean this overwrites the existing document
     * (same ID) if it already exists — safe to call multiple times.
     */
    @JmsListener(destination = "${app.queue.expense-updated}")
    public void onExpenseUpdated(Expense expense) {
        log.info("MQ event received [UPDATED] expenseId={}", expense.getId());
        elasticsearchService.indexExpense(expense);
    }
 
 
    // ─────────────────────────────────────────────────────
    // DELETED
    // ─────────────────────────────────────────────────────
 
    /**
     * Removes a deleted expense from the Elasticsearch index.
     *
     * The publisher sends just the ID as a String (not a full Expense object)
     * because by the time this message is consumed the DB row is already gone —
     * there's nothing else to send. The ID is all ES needs to delete the document.
     */
    @JmsListener(destination = "${app.queue.expense-deleted}")
    public void onExpenseDeleted(String expenseId) {
        log.info("MQ event received [DELETED] expenseId={}", expenseId);
        elasticsearchService.deleteExpense(Long.parseLong(expenseId));
    }
}