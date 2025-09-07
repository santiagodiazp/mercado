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
public class PuntoFarmaScraper extends BaseJsoupScraper {
    private static final String BASE = "https://www.puntofarma.com.py";
    private static final String PATH = "/buscar?s=";

    @Override
    public List<ProductsDto> scrape(String encodedQuery) throws Exception {
        String url = BASE + PATH + encodedQuery;

        Document doc;
        try {
            // First try with normal validation
            doc = fetch(url);
        } catch (SSLHandshakeException ex) {
            // Fallback ONLY for this domain: disable TLS certificate validation
            doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36")
                    .referrer("https://www.google.com")
                    .timeout(8000)
                    .maxBodySize(1_200_000)
                    .followRedirects(true)
                    .method(Connection.Method.GET)
                    .get();
        }

        Elements names = doc.select("a.text-decoration-none > h2");
        Elements prices = doc.select("div.precios-producto > div > span");
        Elements images = doc.select(".align-items-center.mx-auto.card-producto_imagen__EgbD0 > img");

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
