package com.proxyclient.shipproxy;

import com.proxyclient.shipproxy.Controller.ProxyController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static com.proxyclient.shipproxy.constants.ShipProxyCodes.*;

@SpringBootApplication
public class ShipproxyApplication {

    private static final Logger logger = LoggerFactory.getLogger(ShipproxyApplication.class);

    public static void main(String[] args) throws IOException {
        SpringApplication.run(ShipproxyApplication.class, args);
    }

    @Bean
    public Socket offshoreSocket() throws IOException {
        return connectWithRetry();
    }


    @Bean
    public ProxyController proxyController(Socket offshoreSocket) {
        return new ProxyController(offshoreSocket);
    }

    private Socket connectWithRetry() throws IOException {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                logger.info("Attempting to connect to {}:{} (Attempt {})", OFFSHORE_HOST, OFFSHORE_PORT, (attempt + 1));
                System.out.println("Attempting to connect to " + OFFSHORE_HOST + ":" + OFFSHORE_PORT + " (Attempt " + (attempt + 1) + ")");
                Socket s = new Socket(OFFSHORE_HOST, OFFSHORE_PORT);
                logger.info("Connected successfully to offshore proxy!");
                System.out.println("Connected successfully to offshore proxy!");
                return s;
            } catch (IOException e) {
                attempt++;
                if (attempt == MAX_RETRIES) {
                    logger.error("Failed to connect to {}:{} after {} attempts", OFFSHORE_HOST, OFFSHORE_PORT, MAX_RETRIES, e);
                    throw new IOException("Failed to connect to " + OFFSHORE_HOST + ":" + OFFSHORE_PORT + " after " + MAX_RETRIES + " attempts", e);
                }
                logger.warn("Connection failed: {}. Retrying in {}ms...", e.getMessage(), RETRY_DELAY_MS);
                System.out.println("Connection failed: " + e.getMessage() + ". Retrying in " + RETRY_DELAY_MS + "ms...");
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted while waiting to retry", ie);
                    throw new IOException("Interrupted while waiting to retry", ie);
                }
            }
        }
        throw new IOException("Unexpected exit from retry loop");
    }

}
