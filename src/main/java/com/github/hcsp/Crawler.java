package com.github.hcsp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class Crawler extends Thread {

    public static void main(String[] args) throws IOException {
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36";
        String link = "https://sina.cn";

        CloseableHttpClient httpclient = HttpClients.custom().setUserAgent(userAgent).build();
        try (CloseableHttpResponse response = httpclient.execute(new HttpGet(link))) {
            HttpEntity entity = response.getEntity();
            String html = EntityUtils.toString(entity);
            System.out.println("html = " + html);
        }
    }

    @Override
    public void run() {

    }
}
