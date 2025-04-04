package com.proxyclient.shipproxy.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

@RestController
public class ProxyController {

    private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);
    private final Socket offshoreSocket;

    public ProxyController(Socket offshoreSocket) {
        this.offshoreSocket = offshoreSocket;
    }

    @RequestMapping("/**")
    public ResponseEntity<String> proxyRequest(HttpServletRequest request) throws IOException {

        String targetUrl = request.getRequestURL().toString();
        if (request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }

        String requestLine = "GET " + targetUrl + " HTTP/1.1\r\n" +
                "Host: " + new java.net.URL(targetUrl).getHost() + "\r\n" +
                "User-Agent: curl/7.79.1\r\n" +
                "Accept: */*\r\n" +
                "\r\n";
        logger.info("Forwarding request to offshoreserver: {}", requestLine);

        PrintWriter writer = new PrintWriter(offshoreSocket.getOutputStream(), true);
        writer.println(requestLine);

        BufferedReader reader = new BufferedReader(new InputStreamReader(offshoreSocket.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            response.append(line).append("\r\n");
        }
        logger.info("Received response from offshoreserver: {}", response);

        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(response.toString());
    }
}
