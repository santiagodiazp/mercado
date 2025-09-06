package com.example.mercado.scraper;

import com.example.mercado.dto.ProductsDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RealScraper extends BaseJsoupScraper {
    private static final String BASE = "https://www.realonline.com.py";
    private static final String PATH = "/search?name=";

    private static final Pattern TRIPLET = Pattern.compile(
        "(?s)\\{\\s*\"product\"\\s*:\\s*\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"price\"\\s*:\\s*(\\d+)\\s*,\\s*\"photosUrl\"\\s*:\\s*\\[\\s*\"([^\"]+)\""
    );

    @Override
    public List<ProductsDto> scrape(String encodedQuery) throws Exception {
        String url = BASE + PATH + encodedQuery;
        Document doc = fetch(url);

        List<ProductsDto> out = new ArrayList<>();
        for (Element script : doc.select("script")) {
            String raw = script.data();
            if (raw == null || raw.isEmpty()) raw = script.html();
            if (raw == null || raw.isEmpty()) continue;

            String normalized = raw
                .replace("\\u0022", "\"")
                .replace("\\x22", "\"")
                .replace("\\/", "/")
                .replace("\\\"", "\"");

            Matcher m = TRIPLET.matcher(normalized);
            while (m.find()) {
                String name = m.group(1);
                long price = Long.parseLong(m.group(2));
                String photo = m.group(3);
                out.add(product(name, price, "GS", photo, BASE, PATH + encodedQuery));
            }
        }
        return out;
    }
}
