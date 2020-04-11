package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MyBatisCrawlerDao implements CrawlerDao {

    private SqlSessionFactory sqlSessionFactory;

    public MyBatisCrawlerDao() {
        try {
            String resource = "db/mybatis/config.xml";
            InputStream is = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNextLinkThenDelete() throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String url = session.selectOne("com.github.hcsp.NewsMapper.getNextUnvisitedLink");
            if (url != null) {
                session.delete("com.github.hcsp.NewsMapper.deleteLink", url);
            }
            return url;
        }
    }

    @Override
    public void insertNewsIntoDatabase(String url, String title, String content) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.NewsMapper.insertNews", new News(url, title, content));
        }
    }

    @Override
    public void insertLinkIntoDatabase(String link, boolean visited) {
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", visited ? "VISITED_LINKS" : "UNVISITED_LINKS");
        param.put("link", link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.NewsMapper.insertLink", param);
        }
    }

    @Override
    public boolean isVisitedLink(String link) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            int count = (Integer) session.selectOne("com.github.hcsp.NewsMapper.countLink", link);
            return count != 0;
        }
    }
}
