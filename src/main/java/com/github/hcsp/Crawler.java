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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

public class Crawler extends Thread {

    private static final String USERAGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36";
    private static final String INDEX = "https://sina.cn";
    private static final String NEWS = "https://news.sina.cn";

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

    private void parseUrlsFromPageAndStoreIntoDatabase(Document document, Connection connection) {
        Elements aTags = document.select("a");
        for (Element aTag : aTags) {
            String href = aTag.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            }

            if (!isValidateLink(href)) {
                continue;
            }

            insertLinkIntoUnvisited(connection, href);
        }
    }

    private void startCrawling(Connection connection) {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setUserAgent(USERAGENT)
                .build();
        while (true) {
            List<String> unvisitedLinks = loadUrlsFromDatabase(connection, "select link from UNVISITED_LINKS");
            if (unvisitedLinks.isEmpty()) {
                break;
            }

            String link = unvisitedLinks.remove(unvisitedLinks.size() - 1);
            deleteLinkFromDatabase(connection, link);

            if (isVisitedLink(connection, link)) {
                continue;
            }

            Document document = httpGetAndParse(link, httpClient);
            parseUrlsFromPageAndStoreIntoDatabase(document, connection);
            storeOnlyNewsIntoDatabase(document, link);
            insertLinkIntoVisited(connection, link);
        }
    }

    private List<String> loadUrlsFromDatabase(Connection connection, String sql) {
        List<String> results = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteLinkFromDatabase(Connection connection, String link) {
        try (PreparedStatement statement = connection.prepareStatement("delete from UNVISITED_LINKS where link = ?")) {
            statement.setString(1, link);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void insertLinkIntoUnvisited(Connection connection, String link) {
        try (PreparedStatement statement = connection.prepareStatement("insert into UNVISITED_LINKS (link) values (?)")) {
            statement.setString(1, link);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void insertLinkIntoVisited(Connection connection, String link) {
        try (PreparedStatement statement = connection.prepareStatement("insert into VISITED_LINKS (link) values (?)")) {
            statement.setString(1, link);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isVisitedLink(Connection connection, String link) {
        try (PreparedStatement statement = connection.prepareStatement("select link from VISITED_LINKS where link = ?")) {
            statement.setString(1, link);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:file:/Users/samuel/IdeaProjects/crawler-and-elasticsearch/news", "root", "root")) {
            startCrawling(connection);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
