
package com.braincache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point. Like func main() in Go, but this one line boots the whole IoC
 * container: scans com.braincache for @Component/@Service/@Repository/@GrpcService,
 * instantiates them, wires dependencies, starts the gRPC server on :9090.
 * You never `new` your services — the container owns their lifecycle.
 *
 * @EnableScheduling turns on the @Scheduled cron machinery (DailyReviewJob).
 * Go parallel: a background ticker goroutine, but Spring owns the timer thread.
 */
@SpringBootApplication
@EnableScheduling
public class BrainCacheApplication {
    public static void main(String[] args) {
        SpringApplication.run(BrainCacheApplication.class, args);
    }
}
