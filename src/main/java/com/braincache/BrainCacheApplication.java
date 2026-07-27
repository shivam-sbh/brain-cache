package com.braincache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Like func main() in Go, but this one line boots the whole IoC
 * container: scans com.braincache for @Component/@Service/@Repository/@GrpcService,
 * instantiates them, wires dependencies, starts the gRPC server on :9090.
 * You never `new` your services — the container owns their lifecycle.
 */
@SpringBootApplication
public class BrainCacheApplication {
    public static void main(String[] args) {
        SpringApplication.run(BrainCacheApplication.class, args);
    }
}
