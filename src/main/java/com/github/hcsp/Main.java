package com.github.hcsp;

public class Main {

    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            new Crawler(new MyBatisCrawlerDao()).start();
        }
    }
}
