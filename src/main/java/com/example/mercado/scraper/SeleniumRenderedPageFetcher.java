package com.example.mercado.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

final class SeleniumPageFetcher {

    private SeleniumPageFetcher() {}

    static Document fetchRendered(String url, String cssToWaitFor, int timeoutSeconds) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(
                "--headless=new",
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--window-size=1366,768",
                "--lang=es-ES,es;q=0.9,en;q=0.8",
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36",
                "referer=https://www.google.com"
        );
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

        WebDriver driver = new ChromeDriver(options);
        try {
            driver.navigate().to(url);
            if (cssToWaitFor != null && !cssToWaitFor.isEmpty()) {
                new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                        .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(cssToWaitFor)));
            }
            String html = driver.getPageSource();
            return Jsoup.parse(html, url);
        } finally {
            try { driver.quit(); } catch (Exception ignored) {}
        }
    }
}
