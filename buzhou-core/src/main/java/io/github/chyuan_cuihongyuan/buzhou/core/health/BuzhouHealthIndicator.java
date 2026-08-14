package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * {@link BuzhouHealth} → actuator {@link HealthIndicator} 适配（impl-41 / spec 13 §T66；
 * actuator 为 optional 依赖，本类仅在有 actuator 的运行时经装配层条件加载）。
 * UP→UP、DOWN→DOWN、UNKNOWN→UNKNOWN（Spring Boot 健康协议原语）。
 */
public final class BuzhouHealthIndicator implements HealthIndicator {

    private final BuzhouHealth delegate;

    public BuzhouHealthIndicator(BuzhouHealth delegate) {
        this.delegate = delegate;
    }

    @Override
    public Health health() {
        return switch (delegate.status()) {
            case UP -> Health.up().withDetails(detailsSafe()).build();
            case DOWN -> Health.down().withDetails(detailsSafe()).build();
            case UNKNOWN -> Health.unknown().withDetails(detailsSafe()).build();
        };
    }

    private java.util.Map<String, Object> detailsSafe() {
        try {
            return delegate.details();
        } catch (RuntimeException e) {
            return java.util.Map.of("detailsError", String.valueOf(e.getMessage()));
        }
    }
}
