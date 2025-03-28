package com.proxyclient.shipproxy.Controller;

import com.proxyclient.shipproxy.model.ProxyRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;

@RestController
public class ProxyController {

    private final BlockingQueue<ProxyRequest> requestQueue;

    public ProxyController(BlockingQueue<ProxyRequest> requestQueue) {
        this.requestQueue = requestQueue;
    }

    @RequestMapping("/**")
    public ResponseEntity<String> handleRequest(HttpServletRequest request) throws InterruptedException, ExecutionException {

        ProxyRequest proxyRequest = new ProxyRequest(request);
        requestQueue.put(proxyRequest);
        String response = proxyRequest.awaitResponse();
        return ResponseEntity.ok(response);

    }
}
