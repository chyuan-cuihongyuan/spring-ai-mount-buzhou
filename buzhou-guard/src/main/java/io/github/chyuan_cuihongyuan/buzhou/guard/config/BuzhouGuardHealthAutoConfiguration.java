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

    /**
     * spec 344 / T680：审计链完整性巡检（可读 ≠ 完整——断链 DOWN 定位首断点；
     * 超窗 UNKNOWN 带修法）。auditEnabled + store + SigningKeyRing 齐备才装配
     * （无签名密钥 = 降级运行不装配本面——链完整性无从校验）。
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean({
            AuditRecordStore.class, io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyRing.class})
    public io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditChainHealth auditChainHealth(
            org.springframework.core.env.Environment env,
            ObjectProvider<AuditRecordStore> auditStore,
            ObjectProvider<io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyRing> keyRing) {
        boolean guardEnabled = env.getProperty("buzhou.guard.enabled", Boolean.class, true);
        boolean auditEnabled = guardEnabled && env.getProperty(
                "buzhou.guard.audit.enabled", Boolean.class, true);
        return new io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditChainHealth(
                auditEnabled, auditStore.getIfAvailable(), keyRing.getIfAvailable());
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.health.contributor.HealthIndicator")
    static class GuardHealthIndicatorConfiguration {

        @Bean
        public BuzhouHealthIndicator guardHealthIndicator(GuardHealth delegate) {
            return new BuzhouHealthIndicator(delegate);
        }

        @Bean
        public BuzhouHealthIndicator auditChainHealthIndicator(
                io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditChainHealth delegate) {
            return new BuzhouHealthIndicator(delegate);
        }
    }
}
