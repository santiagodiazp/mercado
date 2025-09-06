package com.example.mercado.scraper;

import com.example.mercado.dto.ProductsDto;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SupermasScraper extends BaseJsoupScraper {
    private static final String BASE = "https://www.supermas.com.py";
    private static final String PATH = "/productos?q=";

    @Override
    public List<ProductsDto> scrape(String encodedQuery) throws Exception {
        String url = BASE + PATH + encodedQuery;
        Document doc = fetch(url);

        Elements names = doc.select("h2.woocommerce-loop-product__title");
        Elements prices = doc.select("span.price > span.amount");
        Elements images = doc.select("div.product-list-image > img");

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
