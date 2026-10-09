package com.edstem.interviewprep.shortener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ShortUrlConcurrencyTest {

    private static final int VISITS = 100;
    private static final int THREADS = 20;

    @Autowired
    private ShortUrlService shortUrlService;

    @Test
    void visitCountStaysAccurateUnderSimultaneousVisits() throws Exception {
        String code = shortUrlService.shorten(new ShortenRequest("https://example.com/popular", null)).getCode();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        try {
            for (int i = 0; i < VISITS; i++) {
                results.add(pool.submit(() -> {
                    start.await(); // release all visits at the same moment to maximise contention
                    return shortUrlService.resolveAndCountVisit(code);
                }));
            }
            start.countDown();
            for (Future<String> result : results) {
                assertThat(result.get(30, TimeUnit.SECONDS)).isEqualTo("https://example.com/popular");
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(shortUrlService.stats(code).visitCount()).isEqualTo(VISITS);
    }
}
