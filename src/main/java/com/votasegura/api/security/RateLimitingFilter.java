package com.votasegura.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de seguridad para prevenir ataques de denegación de servicio (DoS),
 * fuerza bruta y abuso de peticiones masivas (Rate Limiting).
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // Límite para endpoints sensibles (Login, Registro, OTP, Recuperación de contraseña)
    private static final int AUTH_LIMIT_PER_MINUTE = 25;
    // Límite general para la API por IP
    private static final int GENERAL_LIMIT_PER_MINUTE = 150;
    private static final long ONE_MINUTE_MS = 60_000L;

    private static class RequestBucket {
        long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        RequestBucket(long now) {
            this.windowStart = now;
            this.count.set(1);
        }
    }

    private final Map<String, RequestBucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, RequestBucket> generalBuckets = new ConcurrentHashMap<>();
    private long lastCleanupTime = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Permitir solicitudes pre-flight CORS siempre
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        
        // Excluir Swagger / OpenAPI y descarga de imágenes estáticas de límites estrictos
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.startsWith("/swagger-resources")
                || (path.startsWith("/api/v1/archivos/") && "GET".equalsIgnoreCase(request.getMethod()))) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();

        // Limpieza periódica cada 5 minutos para evitar acumulación de memoria
        if (now - lastCleanupTime > 300_000L) {
            cleanupExpiredBuckets(now);
            lastCleanupTime = now;
        }

        boolean isAuthEndpoint = path.startsWith("/api/v1/auth/") || (path.startsWith("/api/v1/usuarios") && "POST".equalsIgnoreCase(request.getMethod()));

        if (isAuthEndpoint) {
            if (isRateLimited(authBuckets, "AUTH:" + clientIp, AUTH_LIMIT_PER_MINUTE, now)) {
                sendRateLimitResponse(response, "Has realizado demasiados intentos en poco tiempo. Por seguridad, por favor espera un momento.");
                return;
            }
        }

        // Límite general
        if (isRateLimited(generalBuckets, "GEN:" + clientIp, GENERAL_LIMIT_PER_MINUTE, now)) {
            sendRateLimitResponse(response, "Has superado el límite de solicitudes permitidas. Por favor espera un momento antes de continuar.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(Map<String, RequestBucket> bucketMap, String key, int maxRequests, long now) {
        RequestBucket bucket = bucketMap.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.windowStart) > ONE_MINUTE_MS) {
                return new RequestBucket(now);
            } else {
                existing.count.incrementAndGet();
                return existing;
            }
        });

        return bucket.count.get() > maxRequests;
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", "60");
        String json = String.format("{\"success\":false,\"status\":429,\"message\":\"%s\"}", message);
        response.getWriter().write(json);
    }

    private String getClientIp(HttpServletRequest request) {
        // Cloudflare Tunnel envía la IP real del usuario en CF-Connecting-IP
        String ip = request.getHeader("CF-Connecting-IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }

        // Proxies / Reverse Proxies estándar
        ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            // Si hay múltiples IPs (proxy1, proxy2, ...), la primera es la del cliente
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }

        return request.getRemoteAddr();
    }

    private boolean isValidIp(String ip) {
        return ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip.trim());
    }

    private void cleanupExpiredBuckets(long now) {
        authBuckets.entrySet().removeIf(entry -> (now - entry.getValue().windowStart) > ONE_MINUTE_MS);
        generalBuckets.entrySet().removeIf(entry -> (now - entry.getValue().windowStart) > ONE_MINUTE_MS);
    }
}

