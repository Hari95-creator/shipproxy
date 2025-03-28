package com.proxyclient.shipproxy.model;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ProxyRequest {
    private final String requestData;
    private final CompletableFuture<String> responseFuture = new CompletableFuture<>();

    public ProxyRequest(HttpServletRequest request) {
        this.requestData = request.getMethod() + " " + request.getRequestURL();
    }

    public String serialize() {
        return requestData;
    }

    public void complete(String response) {
        responseFuture.complete(response);
    }

    public String awaitResponse() throws InterruptedException, ExecutionException {
        return responseFuture.get();
    }
}
