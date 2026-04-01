package com.example.expencetracker;


import  static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.expencetracker.dto.ExpenseDTO;
import com.example.expencetracker.exception.ResourceNotFoundException;
import com.example.expencetracker.mapper.ExpenseMapper;
import com.example.expencetracker.model.Expense;
import com.example.expencetracker.repository.ExpenseRepository;
import com.example.expencetracker.service.ExpenseService;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    // ── Mocks ────────────────────────────────────────────
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ExpenseMapper expenseMapper;
    @Mock private JmsTemplate jmsTemplate;

    // ── Class Under Test ─────────────────────────────────
    @InjectMocks
    private ExpenseService expenseService;

    // ── Test Data ────────────────────────────────────────
    private ExpenseDTO.Request sampleRequest;
    private Expense sampleExpense;
    private ExpenseDTO.Response sampleResponse;

    @BeforeEach
    void setUp() {

        // Inject @Value fields manually
        ReflectionTestUtils.setField(expenseService, "createdQueue", "expense.created.queue");
        ReflectionTestUtils.setField(expenseService, "updatedQueue", "expense.updated.queue");
        ReflectionTestUtils.setField(expenseService, "deletedQueue", "expense.deleted.queue");

        // Request DTO
        sampleRequest = new ExpenseDTO.Request();
        sampleRequest.setTitle("Team lunch");
        sampleRequest.setAmount(new BigDecimal("850.00"));
        sampleRequest.setCategory("FOOD");
        sampleRequest.setDate(LocalDate.of(2024, 4, 1));
        sampleRequest.setDescription("Monthly team lunch");

        // Entity
        sampleExpense = new Expense();
        sampleExpense.setId(1L);
        sampleExpense.setTitle("Team lunch");
        sampleExpense.setAmount(new BigDecimal("850.00"));
        sampleExpense.setCategory("FOOD");
        sampleExpense.setDate(LocalDate.of(2024, 4, 1));
        sampleExpense.setDescription("Monthly team lunch");
        sampleExpense.setCreatedAt(LocalDateTime.now());
        sampleExpense.setUpdatedAt(LocalDateTime.now());

        // Response DTO
        sampleResponse = new ExpenseDTO.Response();
        sampleResponse.setId(1L);
        sampleResponse.setTitle("Team lunch");
        sampleResponse.setAmount(new BigDecimal("850.00"));
        sampleResponse.setCategory("FOOD");
        sampleResponse.setDate(LocalDate.of(2024, 4, 1));
        sampleResponse.setDescription("Monthly team lunch");
    }

    // ─────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────
    @Test
    @DisplayName("create() — should save expense and publish MQ event")
    void create_shouldSaveAndPublishEvent() {

        when(expenseMapper.toModel(sampleRequest)).thenReturn(sampleExpense);
        when(expenseRepository.save(sampleExpense)).thenReturn(sampleExpense);
        when(expenseMapper.toResponse(sampleExpense)).thenReturn(sampleResponse);

        ExpenseDTO.Response result = expenseService.create(sampleRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Team lunch");

        verify(expenseRepository, times(1)).save(sampleExpense);
        verify(jmsTemplate, times(1))
                .convertAndSend(eq("expense.created.queue"), any(Expense.class));
    }

    // ─────────────────────────────────────────────────────
    // READ — findAll
    // ─────────────────────────────────────────────────────
    @Test
    @DisplayName("findAll() — should return all expenses")
    void findAll_shouldReturnAllExpenses() {

        when(expenseRepository.findAll()).thenReturn(List.of(sampleExpense));
        when(expenseMapper.toResponse(sampleExpense)).thenReturn(sampleResponse);

        List<ExpenseDTO.Response> results = expenseService.findAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Team lunch");

        verify(expenseRepository).findAll();
    }

    // ─────────────────────────────────────────────────────
    // READ — findById (found)
    // ─────────────────────────────────────────────────────
    @Test
    @DisplayName("findById() — should return expense when found")
    void findById_shouldReturnExpense_whenFound() {

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(sampleExpense));
        when(expenseMapper.toResponse(sampleExpense)).thenReturn(sampleResponse);

        ExpenseDTO.Response result = expenseService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    // ─────────────────────────────────────────────────────
    // READ — findById (not found)
    // ─────────────────────────────────────────────────────
    @Test
    @DisplayName("findById() — should throw exception when not found")
    void findById_shouldThrow_whenNotFound() {

        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────
    @Test
    @DisplayName("update() — should update and publish event")
    void update_shouldUpdateAndPublishEvent() {

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(sampleExpense));
        when(expenseRepository.update(sampleExpense)).thenReturn(sampleExpense);
        when(expenseMapper.toResponse(sampleExpense)).thenReturn(sampleResponse);

        ExpenseDTO.Response result = expenseService.update(1L, sampleRequest);

        assertThat(result).isNotNull();

        verify(expenseRepository).update(sampleExpense);
        verify(jmsTemplate)
                .convertAndSend(eq("expense.updated.queue"), any(Expense.class));
    }

    // ─────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────
    @Test
    @DisplayName("delete() — should delete and publish event")
    void delete_shouldDeleteAndPublishEvent() {

        when(expenseRepository.findById(1L)).thenReturn(Optional.of(sampleExpense));

        expenseService.delete(1L);

        verify(expenseRepository).deleteById(1L);
        verify(jmsTemplate)
                .convertAndSend(eq("expense.deleted.queue"), eq("1"));
    }

    @Test
    @DisplayName("delete() — should throw when not found")
    void delete_shouldThrow_whenNotFound() {

        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(expenseRepository, never()).deleteById(anyLong());
    }
}