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

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

public class Crawler extends Thread {

    private static final String USERAGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36";
    private static final String INDEX = "https://sina.cn";
    private static final String NEWS = "https://news.sina.cn";

    public static void main(String[] args) throws IOException {

        Set<String> visitedLinks = new HashSet<>();
        Deque<String> unvisitedLinks = new ArrayDeque<>();
        unvisitedLinks.add(INDEX);

        CloseableHttpClient httpClient = HttpClients.custom().setUserAgent(USERAGENT).build();
        String link;
        while ((link = unvisitedLinks.poll()) != null) {
            if (visitedLinks.contains(link)) {
                continue;
            }

            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(link))) {
                HttpEntity entity = response.getEntity();
                String html = EntityUtils.toString(entity);

                Document document = Jsoup.parse(html);
                Elements articleTags = document.select("article");
                if (!articleTags.isEmpty()) {
                    String title = articleTags.get(0).child(0).text();
                    System.out.println("title = " + title);
                }

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
                    System.out.println("href = " + href);
                }

                visitedLinks.add(link);
            }
        }
    }

    private static boolean isValidateLink(String link) {
        if (INDEX.equals(link) || link.startsWith(NEWS)) {
            return true;
        }
        return false;
    }

    @Override
    public void run() { }
}
