package com.rush.ticket.catalog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Request correlation + access log.
 * <ul>
 *   <li>Takes {@code X-Request-Id} from the caller (gateway) or generates one, puts it into MDC as
 *       {@code traceId} (the key {@code GlobalExceptionHandler} already reads) and echoes it back in the response,
 *       so a client error report can be matched with server logs by a single id.</li>
 *   <li>Puts {@code userId} (from {@code X-User-Id}) into MDC.</li>
 *   <li>Writes exactly one access-log line per request: method, path, status, duration.</li>
 * </ul>
 * The incoming id is validated: it goes into logs, so arbitrary input would allow log injection.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_USER_ID = "userId";

    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Prometheus scrapes and k8s probes would drown real traffic in the logs
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
        String userId = resolveUserId(request.getHeader(USER_ID_HEADER));

        MDC.put(MDC_TRACE_ID, traceId);
        if (userId != null) {
            MDC.put(MDC_USER_ID, userId);
        }
        response.setHeader(REQUEST_ID_HEADER, traceId);

        long startNanos = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long tookMs = (System.nanoTime() - startNanos) / 1_000_000;
            int status = response.getStatus();
            if (status >= 500) {
                log.warn("HTTP {} {} -> {} in {} ms", request.getMethod(), request.getRequestURI(), status, tookMs);
            } else {
                log.info("HTTP {} {} -> {} in {} ms", request.getMethod(), request.getRequestURI(), status, tookMs);
            }
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    private static String resolveRequestId(String incoming) {
        if (incoming != null && SAFE_REQUEST_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }

    private static String resolveUserId(String incoming) {
        if (incoming == null) {
            return null;
        }
        try {
            return UUID.fromString(incoming).toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
