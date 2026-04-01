package com.example.expencetracker.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.expencetracker.dto.ExpenseDTO;
import com.example.expencetracker.mapper.ExpenseMapper;
import com.example.expencetracker.service.ElasticsearchService;

import java.util.List;
import java.util.stream.Collectors;
 
/**
 * Search endpoint — delegates full-text queries to Elasticsearch.
 *
 * Kept as a separate controller (not merged into ExpenseController) because:
 *   - It has a different data source (ES vs Postgres).
 *   - It may evolve independently (pagination, facets, scoring tweaks).
 *   - Single Responsibility: ExpenseController owns CRUD, this owns search.
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class SearchController {
 
    private final ElasticsearchService elasticsearchService;
    private final ExpenseMapper  expenseMapper;
    
    
    public SearchController(ElasticsearchService elasticsearchService, ExpenseMapper expenseMapper) {
    			this.elasticsearchService = elasticsearchService;
    			this.expenseMapper = expenseMapper;
    			
    }
    
    
    private static final Logger log = LoggerFactory.getLogger(SearchController.class);
 
    /**
     * Full-text search across expense title and description.
     *
     * GET /api/search/expenses?q=lunch
     *
     * Returns a (possibly empty) list of matching expenses ranked by
     * Elasticsearch relevance score.
     */
    @GetMapping("/expenses")
    public ResponseEntity<List<ExpenseDTO.Response>> search(@RequestParam("q") String keyword) {
        log.info("GET /api/search/expenses?q={}", keyword);
 
        List<ExpenseDTO.Response> results = elasticsearchService.search(keyword)
                .stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
 
        return ResponseEntity.ok(results);
    }
}
