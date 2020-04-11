package com.github.hcsp;

import java.sql.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class JdbcCrawlerDao implements CrawlerDao {

    private static final String USER = "root";
    private static final String PASSWORD = "root";
    private Connection connection;

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public JdbcCrawlerDao() {
        try {
            String url = "jdbc:h2:file:/Users/samuel/IdeaProjects/crawler-and-elasticsearch/news";
            connection = DriverManager.getConnection(url, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteLinkFromDatabase(String link) {
        try (PreparedStatement statement = connection.prepareStatement("delete from UNVISITED_LINKS where link = ?")) {
            statement.setString(1, link);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getNextLink() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select link from UNVISITED_LINKS LIMIT 1");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }

    @Override
    public String getNextLinkThenDelete() throws SQLException {
        String link = getNextLink();
        if (link != null) {
            deleteLinkFromDatabase(link);
        }
        return link;
    }

    @Override
    public void insertNewsIntoDatabase(String url, String title, String content) {
        try (PreparedStatement statement = connection.prepareStatement("insert into NEWS (url, title, content) values (?,?,?)")) {
            statement.setString(1, url);
            statement.setString(2, title);
            statement.setString(3, content);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void insertLinkIntoDatabase(String link, boolean visited) {
        String sql = visited ? "insert into VISITED_LINKS (link) values (?)" : "insert into UNVISITED_LINKS (link) values (?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isVisitedLink(String link) throws SQLException {
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
}
