package com.example.sentinel;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.concurrent.ConcurrentLinkedQueue;

@RestController
public class RateLimiterController {

    // Thread-safe storage: IP Address -> Request Count
    private final ConcurrentLinkedQueue<Long> requestTimestamps = new ConcurrentLinkedQueue<>();
    
    // Limit: 10 requests per window
    private final int LIMIT = 2;

    @GetMapping("/api/test")
    public ResponseEntity<String> accessResource(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();

        // if new ip then let it do the requests
        requestTimestamps.add(System.currentTimeMillis());
        // Atomic increment is thread-safe

        int currentRequests = requestTimestamps.size();

        if (currentRequests > LIMIT) {
            Long oldestTimestamp = requestTimestamps.peek();
            if(oldestTimestamp == null) {
                long remainingTime = 60000 - (System.currentTimeMillis() - requestTimestamps.peek());
                return ResponseEntity.status(429)
                    .body("Too Many Requests - Limit reached for IP: " + clientIp + " | Remaining time: " + remainingTime);
            }
            long remainingTime = 60000 - (System.currentTimeMillis() - oldestTimestamp);
            return ResponseEntity.status(429)
                .body("Too Many Requests - Limit reached for IP: " + clientIp + " | Remaining time: " + remainingTime);
        }
        return ResponseEntity.ok("Success! Request count: " + currentRequests  + " | Remaining time: " + (60000 - (System.currentTimeMillis() - requestTimestamps.peek())));
    }


    @Scheduled(fixedRate = 60000)
    public void resetCount(){
        long currentTime = System.currentTimeMillis();
        // Remove requests older than 60 seconds
        requestTimestamps.removeIf(timestamp -> currentTime - timestamp > 60000);
    }
}

