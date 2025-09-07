package com.example.mercado.scraper;

import com.example.mercado.dto.ProductsDto;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLHandshakeException;
import java.util.ArrayList;
import java.util.List;

@Component
public class FarmaciaCatedralScraper extends BaseJsoupScraper {
    private static final String BASE = "https://farmaciacatedral.com.py";
    private static final String PATH = "/buscador?q=";

    @Override
    public List<ProductsDto> scrape(String encodedQuery) throws Exception {
        String url = BASE + PATH + encodedQuery;

        Document doc;
        try {
            // Try regular fetch first
            doc = fetch(url);
        } catch (SSLHandshakeException ex) {
            // If SSL issues appear with regular fetch (unlikely here), still try Selenium
            doc = null;
        }

        // If no content or looks empty, load with Selenium (JS-rendered)
        if (doc == null || doc.select("div.card-producto-texto h2 a").isEmpty()) {
            doc = SeleniumPageFetcher.fetchRendered(url, "div.card-producto-texto h2 a", 15);
        }

        Elements names = doc.select("div.card-producto-texto > h2 > a");
        Elements prices = doc.select("div > div.card-producto-texto > h3");
        Elements images = doc.select("div > a > img");

        int count = Math.min(names.size(), Math.min(prices.size(), images.size()));
        List<ProductsDto> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = names.get(i).text();
            long price = parsePrice(prices.get(i).text());
            String image = resolveImage(images.get(i));
            out.add(product(name, price, "GS", image, BASE, PATH + name));
        }
        return out;
    }
}
