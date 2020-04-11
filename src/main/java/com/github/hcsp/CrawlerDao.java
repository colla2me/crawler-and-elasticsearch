package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLinkThenDelete() throws SQLException;

    void insertNewsIntoDatabase(String url, String title, String content);

    void insertLinkIntoDatabase(String link, boolean visited);

    boolean isVisitedLink(String link) throws SQLException;
}
