package com.dipu.MovieTicketBookingSystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Around("@annotation(idempotent)")
    public Object checkIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String idempotencyKey = request.getHeader(idempotent.headerName());

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed(); // If no key is provided, just proceed (or we could throw an exception if strict)
        }

        String redisKey = "idempotency:" + idempotencyKey;
        String cachedResponse = redisTemplate.opsForValue().get(redisKey);

        if (cachedResponse != null) {
            log.info("Idempotency hit! Returning cached response for key: {}", idempotencyKey);
            Class<?> returnType = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getReturnType();
            
            // If the method returns a ResponseEntity, we need to deserialize its body and reconstruct it
            if (returnType.equals(org.springframework.http.ResponseEntity.class)) {
                // Determine the generic type of the ResponseEntity body (e.g., BookingResponse)
                java.lang.reflect.Type genericReturnType = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod().getGenericReturnType();
                if (genericReturnType instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.Type bodyType = ((java.lang.reflect.ParameterizedType) genericReturnType).getActualTypeArguments()[0];
                    Object body = objectMapper.readValue(cachedResponse, objectMapper.constructType(bodyType));
                    return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(body);
                }
            }
            
            return objectMapper.readValue(cachedResponse, returnType);
        }

        Object result = joinPoint.proceed();

        if (result != null) {
            Object payload = (result instanceof org.springframework.http.ResponseEntity) 
                    ? ((org.springframework.http.ResponseEntity<?>) result).getBody() 
                    : result;
            
            String serializedResponse = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(redisKey, serializedResponse, Duration.ofHours(24));
        }

        return result;
    }
}
