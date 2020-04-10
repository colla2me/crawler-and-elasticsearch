package com.github.hcsp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Crawler extends Thread {

    private static final String USERAGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36";
    private static final String INDEX = "https://sina.cn";
    private static final String NEWS = "https://news.sina.cn";

    private Set<String> visitedLinks = new HashSet<>();
    private Deque<String> unvisitedLinks = new ArrayDeque<>();

    public static void main(String[] args) {
        new Crawler().start();
    }

    private boolean isValidateLink(String link) {
        return INDEX.equals(link) || link.startsWith(NEWS);
    }

    private Document httpGetAndParse(String link, CloseableHttpClient httpClient) {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(link))) {
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity);
            return Jsoup.parse(html);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void storeOnlyNewsIntoDatabase(Document document, String link) {
        Elements articleTags = document.select("article");
        if (!articleTags.isEmpty()) {
            String title = articleTags.get(0).child(0).text();
            String content = articleTags.stream()
                    .flatMap(e -> e.select("p").stream())
                    .map(Element::text)
                    .collect(Collectors.joining("\n"));
            System.out.println("link = " + link);
            System.out.println("title = " + title);
            System.out.println("content = " + content);
        }
    }

    private void parseUrlsFromPageAndStoreIntoDatabase(Document document) {
        Elements aTags = document.select("a");
        for (Element aTag : aTags) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }

            if (!isValidateLink(href)) {
                continue;
            }

            unvisitedLinks.add(href);
        }
    }

    private void startCrawling() {
        CloseableHttpClient httpClient = HttpClients.custom()
                                                    .setUserAgent(USERAGENT)
                                                    .build();
        String link;
        while ((link = unvisitedLinks.poll()) != null) {
            if (visitedLinks.contains(link)) {
                continue;
            }

            Document document = httpGetAndParse(link, httpClient);
            parseUrlsFromPageAndStoreIntoDatabase(document);
            storeOnlyNewsIntoDatabase(document, link);
            visitedLinks.add(link);
        }
    }

    @Override
    public void run() {
        unvisitedLinks.add(INDEX);
        startCrawling();
    }
}
