package com.example.expencetracker.repository;



import com.example.expencetracker.model.Expense;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
* Data access layer for the `expenses` table.
*
* We use NamedParameterJdbcTemplate instead of plain JdbcTemplate because:
*   - Named params (:id, :title) are far more readable than positional ? placeholders.
*   - Eliminates bugs caused by wrong parameter ordering.
*   - Makes SQL strings self-documenting.
*
* No JPA / Hibernate here — JDBC gives us full control over our SQL
* and avoids the N+1 / lazy-loading surprises that come with ORM.
*/
@Slf4j
@Repository
public class ExpenseRepository {

   private final NamedParameterJdbcTemplate jdbc;
   
   
   private static final Logger log = LoggerFactory.getLogger(ExpenseRepository.class);
   
   // ──────────────────────────────────────────────────────
   // Constructor Injection
   // ──────────────────────────────────────────────────────
   public ExpenseRepository(NamedParameterJdbcTemplate jdbc) {
      this.jdbc = jdbc;
      log.debug("ExpenseRepository initialized with NamedParameterJdbcTemplate");
   }

   // ─────────────────────────────────────────────────────
   // RowMapper — converts a JDBC ResultSet row → Expense object
   // Defined once here; reused by every query below.
   // ─────────────────────────────────────────────────────
   private final RowMapper<Expense> expenseRowMapper = (ResultSet rs, int rowNum) -> new Expense(
           rs.getLong("id"),
           rs.getString("title"),
           rs.getBigDecimal("amount"),
           rs.getString("category"),
           rs.getDate("date").toLocalDate(),
           rs.getString("description"),
           rs.getTimestamp("created_at").toLocalDateTime(),
           rs.getTimestamp("updated_at").toLocalDateTime()
   );


   // ─────────────────────────────────────────────────────
   // CREATE
   // ─────────────────────────────────────────────────────

   /**
    * Inserts a new expense row and returns the DB-generated ID back to the caller.
    *
    * KeyHolder captures the auto-generated primary key produced by the DB
    * (SERIAL / IDENTITY column) after the insert.
    */
   public Expense save(Expense expense) {
       String sql = """
               INSERT INTO expenses (title, amount, category, date, description, created_at, updated_at)
               VALUES (:title, :amount, :category, :date, :description, :createdAt, :updatedAt)
               """;

       MapSqlParameterSource params = new MapSqlParameterSource()
               .addValue("title",       expense.getTitle())
               .addValue("amount",      expense.getAmount())
               .addValue("category",    expense.getCategory())
               .addValue("date",        expense.getDate())
               .addValue("description", expense.getDescription())
               .addValue("createdAt",   expense.getCreatedAt())
               .addValue("updatedAt",   expense.getUpdatedAt());

       // KeyHolder captures the auto-generated PK after INSERT
       KeyHolder keyHolder = new GeneratedKeyHolder();
       jdbc.update(sql, params, keyHolder, new String[]{"id"});

       // Attach the generated ID to the model before returning
       expense.setId(keyHolder.getKey().longValue());
       log.debug("Saved new expense with id={}", expense.getId());
       return expense;
   }


   // ─────────────────────────────────────────────────────
   // READ — all rows
   // ─────────────────────────────────────────────────────

   /**
    * Returns every expense, newest first.
    * ORDER BY date DESC makes the most recent entry appear at the top of the list.
    */
   public List<Expense> findAll() {
       String sql = "SELECT * FROM expenses ORDER BY date DESC";
       return jdbc.query(sql, expenseRowMapper);
   }


   // ─────────────────────────────────────────────────────
   // READ — single row by PK
   // ─────────────────────────────────────────────────────

   /**
    * Looks up a single expense by its primary key.
    * Returns Optional.empty() if no row exists for the given id.
    * The service layer converts empty Optional → 404 HTTP response.
    */
   public Optional<Expense> findById(Long id) {
       String sql = "SELECT * FROM expenses WHERE id = :id";

       // queryForObject throws EmptyResultDataAccessException when no row is found.
       // We catch it and convert to Optional.empty() — cleaner than checking size.
       try {
           Expense expense = jdbc.queryForObject(sql,
                   new MapSqlParameterSource("id", id),
                   expenseRowMapper);
           return Optional.ofNullable(expense);
       } catch (org.springframework.dao.EmptyResultDataAccessException e) {
           log.debug("No expense found for id={}", id);
           return Optional.empty();
       }
   }


   // ─────────────────────────────────────────────────────
   // UPDATE
   // ─────────────────────────────────────────────────────

   /**
    * Updates every mutable field of an existing expense row.
    * Returns the updated Expense so the service can broadcast it to MQ.
    */
   public Expense update(Expense expense) {
       String sql = """
               UPDATE expenses
               SET title       = :title,
                   amount      = :amount,
                   category    = :category,
                   date        = :date,
                   description = :description,
                   updated_at  = :updatedAt
               WHERE id = :id
               """;

       MapSqlParameterSource params = new MapSqlParameterSource()
               .addValue("id",          expense.getId())
               .addValue("title",       expense.getTitle())
               .addValue("amount",      expense.getAmount())
               .addValue("category",    expense.getCategory())
               .addValue("date",        expense.getDate())
               .addValue("description", expense.getDescription())
               .addValue("updatedAt",   expense.getUpdatedAt());

       jdbc.update(sql, params);
       log.debug("Updated expense id={}", expense.getId());
       return expense;
   }


   // ─────────────────────────────────────────────────────
   // DELETE
   // ─────────────────────────────────────────────────────

   /**
    * Hard-deletes a row by primary key.
    * The service layer verifies the row exists before calling this.
    */
   public void deleteById(Long id) {
       String sql = "DELETE FROM expenses WHERE id = :id";
       jdbc.update(sql, new MapSqlParameterSource("id", id));
       log.debug("Deleted expense id={}", id);
   }


   // ─────────────────────────────────────────────────────
   // FILTER — category
   // ─────────────────────────────────────────────────────

   /**
    * Fetches all expenses belonging to a specific category.
    * Used by the analytics dashboard to breakdown costs per category.
    */
   public List<Expense> findByCategory(String category) {
       String sql = "SELECT * FROM expenses WHERE category = :category ORDER BY date DESC";
       return jdbc.query(sql, new MapSqlParameterSource("category", category), expenseRowMapper);
   }
}
