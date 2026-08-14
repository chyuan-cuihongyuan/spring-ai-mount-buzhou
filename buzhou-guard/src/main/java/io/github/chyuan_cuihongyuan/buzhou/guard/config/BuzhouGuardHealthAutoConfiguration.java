package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealthIndicator;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditRecordStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyRefresher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * guard 健康装配（impl-41 / spec 13 §T66）：独立于模块开关——禁用/审计关闭报 UNKNOWN；
 * UP = 审计存储读探针通过（无签名密钥/未配策略是降级运行，不是 DOWN）。
 */
@AutoConfiguration
public class BuzhouGuardHealthAutoConfiguration {

    @Bean
    public GuardHealth guardHealth(org.springframework.core.env.Environment env,
            ObjectProvider<AuditRecordStore> auditStore,
            ObjectProvider<PolicyRefresher> policyRefresher) {
        boolean guardEnabled = env.getProperty("buzhou.guard.enabled", Boolean.class, true);
        boolean auditEnabled = guardEnabled && env.getProperty(
                "buzhou.guard.audit.enabled", Boolean.class, true);
        return new GuardHealth(auditEnabled, auditStore.getIfAvailable(),
                policyRefresher.getIfAvailable());
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.health.contributor.HealthIndicator")
    static class GuardHealthIndicatorConfiguration {

        @Bean
        public BuzhouHealthIndicator guardHealthIndicator(GuardHealth delegate) {
            return new BuzhouHealthIndicator(delegate);
        }
    }
}
