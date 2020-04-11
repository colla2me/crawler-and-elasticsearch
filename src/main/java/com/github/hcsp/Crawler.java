package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.stream.Collectors;

public class Crawler extends Thread {

    private static final String USERAGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36";
    private static final String INDEX = "https://sina.cn";
    private static final String NEWS = "https://news.sina.cn";
    private static final String TECH = "https://tech.sina.cn/";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    public static void main(String[] args) {
        new Crawler().start();
    }

    private boolean isValidateLink(String link) {
        return link.startsWith(INDEX) || link.startsWith(NEWS) || link.startsWith(TECH);
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
//            System.out.println("link = " + link);
//            System.out.println("title = " + title);
//            System.out.println("content = " + content);
        }
    }

    private void parseUrlsFromPageAndStoreIntoDatabase(Document document, Connection connection) {
        Elements aTags = document.select("a");
        for (Element aTag : aTags) {
            String href = aTag.attr("href");
            if (href.isEmpty()) {
                continue;
            }

            if (href.startsWith("//")) {
                href = "https:" + href;
            }

            if (!isValidateLink(href)) {
                continue;
            }

            System.out.println("link = " + href);
            insertLinkIntoDatabase(connection, href, "insert into UNVISITED_LINKS (link) values (?)");
        }
    }

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    private void startCrawling() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setUserAgent(USERAGENT)
                .build();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:file:/Users/samuel/IdeaProjects/crawler-and-elasticsearch/news", USER, PASSWORD)){
            String link;
            while ((link = getNextLinkThenDelete(connection)) != null) {
                if (isVisitedLink(connection, link)) {
                    continue;
                }

                Document document = httpGetAndParse(link, httpClient);
                parseUrlsFromPageAndStoreIntoDatabase(document, connection);
                storeOnlyNewsIntoDatabase(document, link);
                insertLinkIntoDatabase(connection, link, "insert into VISITED_LINKS (link) values (?)");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getNextLink(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("select link from UNVISITED_LINKS LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }

    private String getNextLinkThenDelete(Connection connection) throws Exception {
        String link = getNextLink(connection);
        if (link != null) {
            deleteLinkFromDatabase(connection, link);
        }
        return link;
    }

    private void deleteLinkFromDatabase(Connection connection, String link) {
        try (PreparedStatement statement = connection.prepareStatement("delete from UNVISITED_LINKS where link = ?")) {
            statement.setString(1, link);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void insertLinkIntoDatabase(Connection connection, String link, String sql) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isVisitedLink(Connection connection, String link) throws Exception {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("select link from VISITED_LINKS where link = ?")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            return resultSet.next();
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
    }

    @Override
    public void run() {
        startCrawling();
    }
}
