package com.dipu.MovieTicketBookingSystem.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.dipu.MovieTicketBookingSystem.util.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MdcFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String traceId = request.getHeader(AppConstants.TRACE_ID_HEADER);
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString();
            }
            
            MDC.put(AppConstants.MDC_TRACE_ID_KEY, traceId);
            MDC.put(AppConstants.MDC_CLIENT_IP_KEY, request.getRemoteAddr());
            
            // Add trace ID to response so client can track it
            response.addHeader(AppConstants.TRACE_ID_HEADER, traceId);
            
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
