package com.example.mercado.scraper;

import com.example.mercado.dto.ProductsDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseJsoupScraper implements ProductScraper {

    protected Document fetch(String url) throws Exception {
        return Jsoup
            .connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36")
            .referrer("https://www.google.com")
            .timeout(8000)
            .maxBodySize(1_200_000)
            .get();
    }

    protected long parsePrice(String raw) {
        if (raw == null) return 0L;
        String cleaned = raw.replace(".", "").replace(",", "");
        Matcher m = Pattern.compile("(\\d+)").matcher(cleaned);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) { }
        }
        return 0L;
    }

    protected String resolveImage(Element imgEl) {
        if (imgEl == null) return "";
        String[] attrs = {"data-src", "data-original", "src"};
        for (String a : attrs) {
            String abs = imgEl.absUrl(a);
            if (abs != null && !abs.isEmpty()) return abs;
        }
        for (String a : attrs) {
            if (imgEl.hasAttr(a)) {
                String raw = imgEl.attr(a);
                if (raw != null && !raw.isEmpty()) return raw;
            }
        }
        return "";
    }

    protected ProductsDto product(String name, long price, String label, String image, String base, String pathWithQuery) throws Exception {
        return ProductsDto.builder()
            .name(name)
            .price(price)
            .label(label)
            .image(image)
            .origin(new URL(base).getHost())
            .url(base + pathWithQuery)
            .build();
    }
}
