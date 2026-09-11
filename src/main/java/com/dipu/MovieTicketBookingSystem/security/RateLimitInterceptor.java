package com.dipu.MovieTicketBookingSystem.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final int MAX_REQUESTS_PER_MINUTE = 60;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIpAddress(request);
        String redisKey = "rate_limit:" + clientIp;

        try {
            Long requests = redisTemplate.opsForValue().increment(redisKey);
            
            if (requests != null && requests == 1) {
                redisTemplate.expire(redisKey, Duration.ofMinutes(1));
            }

            if (requests != null && requests > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many requests. Please try again in a moment.\"}");
                return false;
            }
        } catch (Exception e) {
            // Fail-open: do not block legitimate requests if Redis is temporarily degraded
            log.warn("Redis rate limit check skipped due to error: {}", e.getMessage());
        }

        return true;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
