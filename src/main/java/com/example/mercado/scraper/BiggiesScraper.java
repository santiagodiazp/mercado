package com.example.mercado.scraper;

import com.example.mercado.dto.ProductsDto;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLHandshakeException;
import java.util.ArrayList;
import java.util.List;

@Component
public class BiggiesScraper extends BaseJsoupScraper {
    private static final String BASE = "https://biggie.com.py";
    private static final String PATH = "/search?q=";

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
        if (doc == null || doc.select("div.v-card__title.titleCard.pt-1").isEmpty()) {
            doc = SeleniumPageFetcher.fetchRendered(url, "div.v-card__title.titleCard.pt-1", 15);
        }

        Elements names = doc.select("div.v-card__title.titleCard.pt-1");
        Elements prices = doc.select("div.v-card__text.title.font-weight-medium.pa-0.d-flex.justify-center > span");
        Elements images = doc.select("div.v-image__image.v-image__image--contain");

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
