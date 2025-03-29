package com.proxyclient.shipproxy;

import com.proxyclient.shipproxy.model.ProxyRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.proxyclient.shipproxy.constants.ShipProxyCodes.*;

@SpringBootApplication
public class ShipproxyApplication {

    private static final BlockingQueue<ProxyRequest> requestQueue = new LinkedBlockingQueue<>();
    private static Socket socket;

    public static void main(String[] args) throws IOException {

        socket = connectWithRetry();
        SpringApplication.run(ShipproxyApplication.class, args);
        new Thread(() -> processQueue(socket)).start();
    }

    private static Socket connectWithRetry() throws IOException {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                System.out.println("Attempting to connect to " + OFFSHORE_HOST + ":" + OFFSHORE_PORT + " (Attempt " + (attempt + 1) + ")");
                Socket s = new Socket(OFFSHORE_HOST, OFFSHORE_PORT);
                System.out.println("Connected successfully to offshore proxy!");
                return s;
            } catch (IOException e) {
                attempt++;
                if (attempt == MAX_RETRIES) {
                    throw new IOException("Failed to connect to " + OFFSHORE_HOST + ":" + OFFSHORE_PORT + " after " + MAX_RETRIES + " attempts", e);
                }
                System.out.println("Connection failed: " + e.getMessage() + ". Retrying in " + RETRY_DELAY_MS + "ms...");
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting to retry", ie);
                }
            }
        }
        throw new IOException("Unexpected exit from retry loop"); // Should never reach here
    }

    private static void processQueue(Socket socket) {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            while (true) {
                ProxyRequest req = requestQueue.take();
                out.println(req.serialize());
                String response = in.readLine();
                req.complete(response);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Bean
    public BlockingQueue<ProxyRequest> requestQueue() {
        return requestQueue;
    }

}
