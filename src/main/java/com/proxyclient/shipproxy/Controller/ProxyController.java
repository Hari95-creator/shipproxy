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
import java.net.SocketTimeoutException;

import static com.proxyclient.shipproxy.constants.ShipProxyCodes.OFFSHORE_HOST;
import static com.proxyclient.shipproxy.constants.ShipProxyCodes.OFFSHORE_PORT;

@RestController
public class ProxyController {
    private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);
    private final Object socketLock = new Object();
    private Socket offshoreSocket;

    public ProxyController(Socket offshoreSocket) {
        this.offshoreSocket = offshoreSocket;
        initializeSocket();
    }

    private void initializeSocket() {
        synchronized (socketLock) {
            try {
                if (offshoreSocket != null && !offshoreSocket.isClosed()) {
                    offshoreSocket.close();
                }
                offshoreSocket = new Socket(OFFSHORE_HOST, OFFSHORE_PORT);
                offshoreSocket.setSoTimeout(15000); // Increased to 15 seconds
                logger.info("Socket to offshoreserver initialized with timeout 15s");
            } catch (IOException e) {
                logger.error("Failed to initialize socket to offshoreserver", e);
                throw new RuntimeException("Socket initialization failed", e);
            }
        }
    }

    @RequestMapping("/**")
    public ResponseEntity<String> proxyRequest(HttpServletRequest request) throws IOException {
        long startTime = System.currentTimeMillis();
        logger.info("Received request from client for URL: {}", request.getRequestURL());

        synchronized (socketLock) {
            if (offshoreSocket == null || offshoreSocket.isClosed() || !offshoreSocket.isConnected()) {
                logger.warn("Socket to offshoreserver is invalid, reinitializing...");
                initializeSocket();
            }
        }
        String targetUrl = request.getRequestURL().toString();
        if (request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }

        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append(request.getMethod()).append(" ").append(targetUrl).append(" HTTP/1.1\r\n");
        requestBuilder.append("Host: ").append(new java.net.URL(targetUrl).getHost()).append("\r\n");
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            if (!headerName.equalsIgnoreCase("host")) {
                requestBuilder.append(headerName).append(": ").append(request.getHeader(headerName)).append("\r\n");
            }
        });
        requestBuilder.append("\r\n");

        String requestLine = requestBuilder.toString();
        logger.info("Forwarding request to offshoreserver: {}", requestLine);
        long sendStartTime = System.currentTimeMillis();
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(offshoreSocket.getOutputStream(), true);
            writer.println(requestLine);
        } catch (IOException e) {
            logger.error("Failed to send request to offshoreserver", e);
            synchronized (socketLock) {
                if (writer != null) writer.close();
                initializeSocket();
                writer = new PrintWriter(offshoreSocket.getOutputStream(), true);
                writer.println(requestLine);
            }
        }
        long sendEndTime = System.currentTimeMillis();
        logger.info("Time to send request to offshoreserver: {} ms", (sendEndTime - sendStartTime));
        long readStartTime = System.currentTimeMillis();
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();
        String line;
        boolean headersEnded = false;
        try {
            reader = new BufferedReader(new InputStreamReader(offshoreSocket.getInputStream()));
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() && !headersEnded) {
                    headersEnded = true;
                    continue;
                }
                response.append(line).append("\r\n");
                logger.debug("Received line from offshoreserver: {}", line);
            }
        } catch (SocketTimeoutException e) {
            logger.error("Timeout reading response from offshoreserver", e);
            throw new IOException("Timeout reading response", e);
        } catch (IOException e) {
            logger.error("Error reading response from offshoreserver", e);
            throw e;
        } finally {
            if (reader != null) reader.close();
        }
        long readEndTime = System.currentTimeMillis();
        logger.info("Time to read response from offshoreserver: {} ms", (readEndTime - readStartTime));
        logger.info("Received full response from offshoreserver: {}", response);

        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("Total time to process request: {} ms", totalTime);

        return ResponseEntity.ok().header("Content-Type", "text/html; charset=UTF-8").body(response.toString());
    }
}