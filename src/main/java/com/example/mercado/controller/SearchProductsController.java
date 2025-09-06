package com.example.mercado.controller;

import com.example.mercado.dto.ProductsDto;
import com.example.mercado.dto.SearchDto;
import com.example.mercado.scraper.ProductScraper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
// add this import
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
public class SearchProductsController {

    private final Logger logger = LoggerFactory.getLogger(SearchProductsController.class);

    private final List<ProductScraper> scrapers;

    // Simple in-memory cache with TTL per query
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
    private record CacheEntry(List<ProductsDto> data, long timestamp) {}
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // Parallel executor
    private final ExecutorService fetchPool = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors() / 2)
    );

    public SearchProductsController(List<ProductScraper> scrapers) {
        this.scrapers = scrapers;
    }

    @PreDestroy
    void shutdown() {
        fetchPool.shutdown();
    }

    @GetMapping("/")
    public String search(Model model) {
        model.addAttribute("products", new ArrayList<>());
        model.addAttribute("searchForm", new SearchDto());
        return "index";
    }

    @PostMapping("/")
    public String listStudent(@ModelAttribute("searchForm") SearchDto request, Model model) {
        try {
            if (request == null || request.getSearch() == null || request.getSearch().trim().isEmpty()) {
                model.addAttribute("products", new ArrayList<>());
                model.addAttribute("searchForm", request == null ? new SearchDto() : request);
                model.addAttribute("errorMessage", "Ingresa un término de búsqueda.");
                return "index";
            }

            String rawQuery = request.getSearch().trim();
            String encodedQuery = URLEncoder.encode(rawQuery, StandardCharsets.UTF_8);

            // Cache key (normalized)
            String cacheKey = rawQuery.toLowerCase();

            // 1) Try cache hit
            CacheEntry cached = cache.get(cacheKey);
            if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL_MS) {
                logger.info("Cache hit for query: {}", rawQuery);
                model.addAttribute("products", cached.data);
                model.addAttribute("searchForm", request);
                return "index";
            }

            // 2) Fetch from all scrapers in parallel
            List<CompletableFuture<List<ProductsDto>>> futures = scrapers.stream()
                    .map(scraper -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return scraper.scrape(encodedQuery);
                        } catch (Exception ex) {
                            logger.warn("Scraper {} failed: {}", scraper.getClass().getSimpleName(), ex.toString());
                            // use typed empty list
                            return Collections.<ProductsDto>emptyList();
                        }
                    }, fetchPool))
                    .toList();

            List<ProductsDto> merged = futures.stream()
                    .map(f -> {
                        try {
                            return f.get(12, TimeUnit.SECONDS);
                        } catch (Exception ex) {
                            return List.<ProductsDto>of();
                        }
                    })
                    .flatMap(List::stream)
                    .toList();

            // Filter and sort
            Pattern p = Pattern.compile(Pattern.quote(rawQuery), Pattern.CASE_INSENSITIVE);
            List<ProductsDto> productsDtoList = merged.stream()
                    .filter(c -> c.getName() != null && p.matcher(c.getName()).find())
                    .sorted(Comparator.comparing(ProductsDto::getPrice)
                            .thenComparing(ProductsDto::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            // 3) Store in cache
            cache.put(cacheKey, new CacheEntry(productsDtoList, System.currentTimeMillis()));

            model.addAttribute("products", productsDtoList);
            model.addAttribute("searchForm", request);
            return "index";
        } catch (Exception e) {
            logger.error("Search failed", e);
            model.addAttribute("products", new ArrayList<>());
            model.addAttribute("searchForm", request == null ? new SearchDto() : request);
            model.addAttribute("errorMessage", "No pudimos completar la búsqueda. Intenta de nuevo más tarde.");
            return "index";
        }
    }
}
