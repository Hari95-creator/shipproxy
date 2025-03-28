package com.proxyclient.shipproxy;

import com.proxyclient.shipproxy.constants.ShipProxyCodes;
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
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

@SpringBootApplication
public class ShipproxyApplication {

    private static final BlockingQueue<ProxyRequest> requestQueue = new LinkedBlockingQueue<>();
    private static Socket socket;

    public static void main(String[] args) throws IOException {

        socket = new Socket(ShipProxyCodes.OFFSHORE_HOST, ShipProxyCodes.OFFSHORE_PORT);
        SpringApplication.run(ShipproxyApplication.class, args);
        new Thread(() -> processQueue(socket)).start();
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
