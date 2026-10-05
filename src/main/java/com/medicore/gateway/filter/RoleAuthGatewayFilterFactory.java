package com.medicore.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Role-based authorization at the edge, per route.
 * Configured in application.yml via: filters: - RoleAuth=ADMIN
 */
@Component
public class RoleAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<RoleAuthGatewayFilterFactory.Config> {

    public RoleAuthGatewayFilterFactory() {
        super(Config.class);
    }

    /**
     * Declares which config field the shorthand value binds to, so that
     * "RoleAuth=ADMIN,PATIENT" populates allowedRoles (comma-separated list).
     * Without this override Spring Cloud Gateway cannot bind the shorthand
     * and the filter NPEs on a null list.
     */
    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("allowedRoles");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String role = exchange.getRequest().getHeaders().getFirst("X-User-Role");
            List<String> allowed = normalize(config.getAllowedRoles());
            if (allowed.isEmpty()) {
                // Fail closed: a misconfigured route must never be remotely reachable.
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                return exchange.getResponse().setComplete();
            }
            if (role == null || !allowed.contains(role)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };
    }

    /**
     * Both "RoleAuth=ADMIN" and "RoleAuth=ADMIN,PATIENT" work: depending on how
     * the shorthand is converted, comma-joined values can arrive as a single
     * element, so every element is split on commas before matching.
     */
    private static List<String> normalize(List<String> raw) {
        if (raw == null) return List.of();
        return raw.stream()
                .flatMap(v -> Arrays.stream(v.split(",")))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .map(String::toUpperCase)
                .toList();
    }

    public static class Config {
        private List<String> allowedRoles;

        public List<String> getAllowedRoles() {
            return allowedRoles;
        }

        public void setAllowedRoles(List<String> allowedRoles) {
            this.allowedRoles = allowedRoles;
        }
    }
}
