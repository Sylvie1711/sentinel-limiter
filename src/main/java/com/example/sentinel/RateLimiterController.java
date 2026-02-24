package com.example.sentinel;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.scheduling.annotation.Scheduled;

@RestController
public class RateLimiterController {

    // 🗺️ Maps IP address to a personal queue of request timestamps
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> storage = new ConcurrentHashMap<>();
    private final int LIMIT = 10;
    private final long WINDOW_MS = 60000;

    @GetMapping("/api/test")
    public ResponseEntity<String> accessResource(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        long now = System.currentTimeMillis();

        // 1. TODO: Use computeIfAbsent to get the queue for this IP
        ConcurrentLinkedQueue<Long> userQueue = storage.computeIfAbsent(clientIp, k -> new ConcurrentLinkedQueue<>());

        // 2. TODO: Write a 'while' loop to remove timestamps older than (now - WINDOW_MS)
        // Hint: Use userQueue.peek() to check the oldest and userQueue.poll() to remove it
        while( userQueue.peek() != null && userQueue.peek() < now - WINDOW_MS ){
            userQueue.poll();
        }
        

        // 3. TODO: Check if userQueue.size() is less than LIMIT
        if (userQueue.size() < LIMIT || userQueue.isEmpty()) {
            // 4. TODO: Add 'now' to the queue and return a success response
            userQueue.add(now);
            return ResponseEntity.ok("Success!");
        } else {
            // Return a 429 error if over limit
            long retryAfter = WINDOW_MS - (now - userQueue.peek());
            return ResponseEntity.status(429)
                .body("Too Many Requests. Try again in " + (retryAfter / 1000) + "s");
        }
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void cleanupInactiveUsers() {
        long now = System.currentTimeMillis();
        long inactivityThreshold = 300000; // 5 minutes

        storage.entrySet().removeIf(entry -> {
            ConcurrentLinkedQueue<Long> queue = entry.getValue();
            Long lastRequest = queue.peek(); // How do we get the NEWEST timestamp?
            
            // Return true if the user has been inactive longer than the threshold
            if(queue.isEmpty() || lastRequest == null || (now - lastRequest > inactivityThreshold)){
                return true;
            }else return false;
        });
    }
}
