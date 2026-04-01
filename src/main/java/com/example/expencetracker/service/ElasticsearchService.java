package com.example.expencetracker.service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.expencetracker.model.Expense;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
* Manages Expense documents in the Elasticsearch index.
*
* Elasticsearch role in this system:
*   - Full-text search across title and description fields.
*   - Fast filtering by category, date range, amount range.
*   - The Postgres DB remains the source of truth; ES is a read-optimised
*     projection of that data, kept in sync via the MQ listener.
*
* Flow:
*   Write to DB → publish to MQ → MQ listener calls this service → ES updated.
*   Read (search) → this service → ES → results returned to client.
*/
@Service
public class ElasticsearchService {

   private final ElasticsearchClient esClient;
   
   public ElasticsearchService(ElasticsearchClient esClient) {
	   this.esClient = esClient;
   }
   
   
   private static final Logger log = LoggerFactory.getLogger(ElasticsearchService.class);

   // Index name injected from application.yml → elasticsearch.index.expense
   @Value("${elasticsearch.index.expense}")
   private String expenseIndex;


   // ─────────────────────────────────────────────────────
   // INDEX (create or update a document)
   // ─────────────────────────────────────────────────────

   /**
    * Indexes (upserts) an Expense document in Elasticsearch.
    * The document ID is the expense's DB primary key, cast to String,
    * so a re-index of the same expense ID always overwrites the old document.
    *
    * Called after both CREATE and UPDATE operations.
    */
   public void indexExpense(Expense expense) {
       try {
           IndexRequest<Expense> request = IndexRequest.of(builder -> builder
                   .index(expenseIndex)
                   .id(String.valueOf(expense.getId()))  // Use DB PK as ES document ID
                   .document(expense)                    // Serialize the full Expense object
           );

           esClient.index(request);
           log.debug("Indexed expense id={} into ES index={}", expense.getId(), expenseIndex);

       } catch (IOException e) {
           // Log and swallow — ES sync failure should not break the primary API response
           log.error("Failed to index expense id={}: {}", expense.getId(), e.getMessage(), e);
       }
   }


   // ─────────────────────────────────────────────────────
   // DELETE a document
   // ─────────────────────────────────────────────────────

   /**
    * Removes an expense document from the ES index by its DB ID.
    * Called after a DELETE operation to keep ES in sync with Postgres.
    */
   public void deleteExpense(Long id) {
       try {
           DeleteRequest request = DeleteRequest.of(builder -> builder
                   .index(expenseIndex)
                   .id(String.valueOf(id))
           );

           esClient.delete(request);
           log.debug("Deleted expense id={} from ES index={}", id, expenseIndex);

       } catch (IOException e) {
           log.error("Failed to delete expense id={} from ES: {}", id, e.getMessage(), e);
       }
   }


   // ─────────────────────────────────────────────────────
   // SEARCH
   // ─────────────────────────────────────────────────────

   /**
    * Performs a full-text search across the `title` and `description` fields.
    *
    * multi_match query: searches the same keyword across multiple fields,
    * ranking results by combined relevance score.
    *
    * Returns a list of Expense objects hydrated from the ES _source.
    * The caller (controller) can then map these to response DTOs.
    *
    * @param keyword — the search term entered by the user
    */
   public List<Expense> search(String keyword) {
       try {
           SearchResponse<Expense> response = esClient.search(builder -> builder
                   .index(expenseIndex)
                   .query(q -> q
                           .multiMatch(mm -> mm
                                   .query(keyword)
                                   // Search both human-readable fields
                                   .fields("title", "description")
                           )
                   ),
                   Expense.class   // Deserialize _source JSON into Expense objects
           );

           // Extract the Expense from each search hit
           return response.hits().hits().stream()
                   .map(Hit::source)
                   .collect(Collectors.toList());

       } catch (IOException e) {
           log.error("Search failed for keyword='{}': {}", keyword, e.getMessage(), e);
           return Collections.emptyList(); // Degrade gracefully — return empty rather than 500
       }
   }
}