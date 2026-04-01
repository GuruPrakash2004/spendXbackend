package com.example.expencetracker.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.expencetracker.dto.ExpenseDTO;
import com.example.expencetracker.service.ExpenseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "http://localhost:5173")
public class ExpenseController {

    private final ExpenseService expenseService;
    
    
    public ExpenseController(ExpenseService expenseService) {
		this.expenseService = expenseService;
	}
    
    private static final Logger log = LoggerFactory.getLogger(ExpenseController.class);

    @PostMapping
    public ResponseEntity<ExpenseDTO.Response> create(
            @Valid @RequestBody ExpenseDTO.Request request) {

        log.info("Creating new expense");

        ExpenseDTO.Response response = expenseService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ExpenseDTO.Response>> getAll(
            @RequestParam(required = false) String category) {

        log.info("Fetching expenses. category={}", category);

        List<ExpenseDTO.Response> expenses;

        if (category != null && !category.isBlank()) {
            expenses = expenseService.findByCategory(category);
        } else {
            expenses = expenseService.findAll();
        }

        return ResponseEntity.ok(expenses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseDTO.Response> getById(@PathVariable Long id) {

        log.info("Fetching expense id={}", id);

        return ResponseEntity.ok(expenseService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseDTO.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseDTO.Request request) {

        log.info("Updating expense id={}", id);

        return ResponseEntity.ok(expenseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        log.info("Deleting expense id={}", id);

        expenseService.delete(id);

        return ResponseEntity.noContent().build();
    }
}