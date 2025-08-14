package org.example;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class Main {
    
    public static void main(String[] args) throws InterruptedException {
        org.springframework.boot.SpringApplication.run(Main.class, args);
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));
        latch.await();
    }
}