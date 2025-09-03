package com.example.mercado.controller;

import com.example.mercado.dto.ProductsDto;
import com.example.mercado.dto.SearchDto;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
public class SearchProductsController {

    private final Logger logger = LoggerFactory.getLogger(SearchProductsController.class);

    public static final String SUPERSEIS = "https://www.superseis.com.py/search.aspx?searchterms=";
    public static final String CASARICA = "https://www.casarica.com.py/productos?q=";
    public static final String STOCK = "https://www.stock.com.py/search.aspx?searchterms=";
    public static final String REAL = "https://www.realonline.com.py/search?name=";
    public static final String SALEMMA = "https://www.salemmaonline.com.py/buscar?q=";
    public static final String SUPERMAS = "https://www.supermas.com.py/productos?q=";

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

            String withoutEncode = request.getSearch();
            String search = encodeValue(withoutEncode);

            List<String> clients = new ArrayList<>();
            clients.add(SUPERSEIS);
            clients.add(CASARICA);
            clients.add(STOCK);
            clients.add(REAL);
            clients.add(SALEMMA);
            clients.add(SUPERMAS);

            List<ProductsDto> objectList = new ArrayList<>();
            for (String client : clients) {
                List<ProductsDto> productsDtos = getProductsDtos(search, client);
                objectList.addAll(productsDtos);
            }

            objectList.sort(Comparator.comparing(ProductsDto::getName));
            objectList.sort(Comparator.comparing(ProductsDto::getPrice));

            Pattern p = Pattern.compile(".*" + withoutEncode + ".*", Pattern.CASE_INSENSITIVE);

            List<ProductsDto> productsDtoList = objectList
                    .stream()
                    .filter(c -> p.matcher(c.getName()).matches())
                    .collect(Collectors.toList());

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

    private String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    private List<ProductsDto> getProductsDtos(String search, String client) throws IOException {
        List<ProductsDto> productsDtos = new ArrayList<>();
        Document document = Jsoup.connect(client + search).get();
        Elements labels = new Elements();
        Elements prices = new Elements();
        Elements products = new Elements();
        Elements images = new Elements();
        if (SUPERSEIS.equals(client)) {
            products.addAll(document.select("h2.product-title"));
            prices.addAll(document.select("span.price-label"));
            images.addAll(document.select("a.picture-link > img"));
        }
        if (CASARICA.equals(client)) {
            products.addAll(document.select("h2.ecommercepro-loop-product__title"));
            prices.addAll(document.select("span.price > span.amount"));
            images.addAll(document.select("div.product-list-image > img"));
        }
        if (STOCK.equals(client)) {
            products.addAll(document.select("h2.product-title"));
            prices.addAll(document.select("span.price-label"));
            images.addAll(document.select("a.picture-link > img"));
        }
        if (REAL.equals(client)) {
            Pattern triplet = Pattern.compile(
                    "(?s)\\{\\s*\"product\"\\s*:\\s*\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"price\"\\s*:\\s*(\\d+)\\s*,\\s*\"photosUrl\"\\s*:\\s*\\[\\s*\"([^\"]+)\""
            );

            for (Element script : document.select("script")) {
                String raw = script.data();
                if (raw.isEmpty()) raw = script.html();
                if (raw.isEmpty()) continue;

                // Normalize common escapes from JS/Next.js
                String normalized = raw
                        .replace("\\u0022", "\"")
                        .replace("\\x22", "\"")
                        .replace("\\/", "/")
                        .replace("\\\"", "\"");

                Matcher m = triplet.matcher(normalized);
                while (m.find()) {
                    String name  = m.group(1);
                    long price   = Long.parseLong(m.group(2));
                    String photo = m.group(3);
                    ProductsDto p = ProductsDto
                            .builder()
                            .name(name)
                            .price(price)
                            .label("GS")
                            .image(photo)
                            .origin(new URL(client).getHost())
                            .url(client)
                            .build();
                    productsDtos.add(p);
                }
            }

        }
        if (SALEMMA.equals(client)) {
            products.addAll(document.select("a.apsubtitle"));
            prices.addAll(document.select("h6[class=pprice]"));
            images.addAll(document.select("img.imgprodts"));
        }
        if (SUPERMAS.equals(client)) {
            products.addAll(document.select("h2.woocommerce-loop-product__title"));
            prices.addAll(document.select("span.price > span.amount"));
            images.addAll(document.select("div.product-list-image > img"));
        }
        logger.info(client);
        logger.info("products: " + products.size());
        logger.info("prices: " + prices.size());
        logger.info("images: " + images.size());
        for (int i = 0; i < products.size(); i++) {
            String text = "NN";
            String text1 = "NN";
            String text2 = "NN";
            try {
                text = products.get(i).text();
                text1 = prices.get(i).text()
                        .replace(".", "")
                        .replace("₲", "")
                        .replace("Gs", "")
                        .replace("el KG", "")
                        .trim();
                text2 = images.get(i).attr("data-src") + images.get(i).attr("src");
            } catch (Exception e) {
                logger.error(client);
                logger.error(e.getMessage());
            }
            ProductsDto p = ProductsDto
                    .builder()
                    .name(text)
                    .price(Strings.isEmpty(text1) ? 0L : Long.parseLong(text1))
                    .label("GS")
                    .image(text2)
                    .origin(new URL(client).getHost())
                    .url(client)
                    .build();
            productsDtos.add(p);
        }

        return productsDtos;
    }
}
