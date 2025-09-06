package com.example.mercado.scraper;

import com.example.mercado.dto.ProductsDto;

import java.util.List;

public interface ProductScraper {
    /**
     * Scrape products for the given URL-encoded query.
     * @param encodedQuery query already encoded with UTF-8
     * @return list of products found
     */
    List<ProductsDto> scrape(String encodedQuery) throws Exception;
}
