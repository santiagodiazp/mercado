package com.example.mercado.scraper;

import com.example.mercado.dto.ProductsDto;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.List;

@Component
public class FarmacenterScraper extends BaseJsoupScraper {
    private static final String BASE = "https://www.farmacenter.com.py";
    private static final String PATH = "/catalogo?q=";

    // Lazily initialized, only used for Farmacenter connections
    private volatile SSLSocketFactory farmacenterSslFactory;

    @Override
    public List<ProductsDto> scrape(String encodedQuery) throws Exception {
        String url = BASE + PATH + encodedQuery;

        Document doc;
        try {
            // First try with normal validation
            doc = fetch(url);
        } catch (SSLHandshakeException ex) {
            // Retry ONLY for this domain using a PEM-backed trust store via a custom SSL context
            doc = Jsoup.connect(url)
                    .sslSocketFactory(getFarmacenterSslFactory())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36")
                    .referrer("https://www.google.com")
                    .timeout(8000)
                    .maxBodySize(1_200_000)
                    .followRedirects(true)
                    .method(Connection.Method.GET)
                    .get();
        }

        Elements names = doc.select("h2.ecommercepro-loop-product__title");
        Elements prices = doc.select("span.price > span");
        Elements images = doc.select("img.wp-post-image");

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

    // Build an SSL socket factory that trusts the Farmacenter certificate (PEM) only
    private SSLSocketFactory getFarmacenterSslFactory() throws Exception {
        SSLSocketFactory local = farmacenterSslFactory;
        if (local != null) return local;

        synchronized (this) {
            if (farmacenterSslFactory != null) return farmacenterSslFactory;

            // Load PEM from classpath: src/main/resources/ssl/farmacenter.pem
            byte[] pemBytes;
            try (InputStream is = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("ssl/farmacenter.pem")) {
                if (is == null) {
                    throw new IllegalStateException("PEM certificate ssl/farmacenter.pem not found on classpath");
                }
                pemBytes = is.readAllBytes();
            }

            // Parse one or multiple PEM certificates into an in-memory KeyStore
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, null);

            String pem = new String(pemBytes, StandardCharsets.US_ASCII);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            String begin = "-----BEGIN CERTIFICATE-----";
            String end = "-----END CERTIFICATE-----";
            int idx = 0, pos = 0;
            boolean any = false;
            while (true) {
                int start = pem.indexOf(begin, pos);
                if (start < 0) break;
                int finish = pem.indexOf(end, start);
                if (finish < 0) break;
                String block = pem.substring(start, finish + end.length());
                X509Certificate cert = (X509Certificate)
                        cf.generateCertificate(new ByteArrayInputStream(block.getBytes(StandardCharsets.US_ASCII)));
                ks.setCertificateEntry("farmacenter-" + (idx++), cert);
                pos = finish + end.length();
                any = true;
            }
            if (!any) {
                throw new IllegalStateException("No X.509 certificate blocks found in ssl/farmacenter.pem");
            }

            TrustManagerFactory tmf =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), new SecureRandom());

            farmacenterSslFactory = ctx.getSocketFactory();
            return farmacenterSslFactory;
        }
    }
}
