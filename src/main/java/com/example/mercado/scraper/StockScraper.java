package com.example.mercado.scraper;

import com.example.mercado.dto.ProductsDto;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StockScraper extends BaseJsoupScraper {
    private static final String BASE = "https://www.stock.com.py";
    private static final String PATH = "/search.aspx?searchterms=";

    @Override
    public List<ProductsDto> scrape(String encodedQuery) throws Exception {
        String url = BASE + PATH + encodedQuery;
        Document doc = fetch(url);

        Elements names = doc.select("h2.product-title");
        Elements prices = doc.select("span.price-label");
        Elements images = doc.select("a.picture-link > img");

        int count = Math.min(names.size(), Math.min(prices.size(), images.size()));
        List<ProductsDto> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = names.get(i).text();
            long price = parsePrice(prices.get(i).text());
            String image = resolveImage(images.get(i));
            out.add(product(name, price, "GS", image, BASE, PATH + encodedQuery));
        }
        return out;
    }
}
