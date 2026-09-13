package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealthIndicator;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AgentAuditRecord;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditRecordStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.InMemoryAuditRecordStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyRing;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BuzhouGuardHealthAutoConfiguration 直测（spec 1200 / T1801 / K 会话 R1 补测——此前零覆盖）。
 *
 * <p>ApplicationContextRunner 装配面 + GuardHealth 三态语义（UP=审计存储可读 /
 * UNKNOWN=禁用或缺存储 / DOWN=存储读探针抛）+ indicator 透传。
 */
class BuzhouGuardHealthAutoConfigurationTest {

    private static final String GUARD_ENABLED_KEY = "buzhou.guard.enabled";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouGuardHealthAutoConfiguration.class));

    /** count() 必抛的审计存储 stub（DOWN 路径——核心职能「可读」不可用）。 */
    private static final class BrokenAuditRecordStore implements AuditRecordStore {
        @Override
        public void append(AgentAuditRecord record) {
            throw new IllegalStateException("审计存储断连");
        }

        @Override
        public List<AgentAuditRecord> loadAll() {
            throw new IllegalStateException("审计存储断连");
        }

        @Override
        public long count() {
            throw new IllegalStateException("审计存储断连");
        }
    }

    @Test
    void guardHealthAssemblesWithoutSpiBeansAndReportsUnknown() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(GuardHealth.class);
            // 无 AuditRecordStore → auditChainHealth 不装配（@ConditionalOnBean）
            assertThat(ctx).doesNotHaveBean("auditChainHealth");
            // T1803：indicator 内部类随同一条件——delegates 缺席时整体跳过（修复前此处启动崩溃）
            assertThat(ctx).doesNotHaveBean(BuzhouHealthIndicator.class);
            GuardHealth guardHealth = ctx.getBean(GuardHealth.class);
            assertThat(guardHealth.status()).isEqualTo(BuzhouHealth.Status.UNKNOWN);
            assertThat(guardHealth.details()).containsEntry("auditEnabled", true);
        });
    }

    @Test
    void auditChainHealthAssemblesWhenStoreAndKeyRingPresent() {
        runner.withBean(AuditRecordStore.class, InMemoryAuditRecordStore::new)
                .withBean(SigningKeyRing.class, SigningKeyRing::new)
                .run(ctx -> {
                    GuardHealth guardHealth = ctx.getBean(GuardHealth.class);
                    assertThat(guardHealth.status()).isEqualTo(BuzhouHealth.Status.UP);
                    assertThat(ctx).hasBean("auditChainHealth");
                    // spring-boot-health 在测试 classpath → 两个 indicator 装配并透传状态
                    assertThat(ctx).hasBean("guardHealthIndicator");
                    assertThat(ctx).hasBean("auditChainHealthIndicator");
                    BuzhouHealthIndicator indicator =
                            (BuzhouHealthIndicator) ctx.getBean("guardHealthIndicator");
                    assertThat(indicator.health().getStatus().getCode()).isEqualTo("UP");
                });
    }

    @Test
    void unreadableAuditStoreReportsDownThroughIndicator() {
        runner.withBean(AuditRecordStore.class, BrokenAuditRecordStore::new)
                .withBean(SigningKeyRing.class, SigningKeyRing::new)
                .run(ctx -> {
                    GuardHealth guardHealth = ctx.getBean(GuardHealth.class);
                    assertThat(guardHealth.status()).isEqualTo(BuzhouHealth.Status.DOWN);
                    BuzhouHealthIndicator indicator =
                            (BuzhouHealthIndicator) ctx.getBean("guardHealthIndicator");
                    assertThat(indicator.health().getStatus().getCode()).isEqualTo("DOWN");
                });
    }

    @Test
    void guardDisabledReportsUnknownWithDisabledDetail() {
        runner.withPropertyValues(GUARD_ENABLED_KEY + "=false")
                .withBean(AuditRecordStore.class, InMemoryAuditRecordStore::new)
                .run(ctx -> {
                    GuardHealth guardHealth = ctx.getBean(GuardHealth.class);
                    assertThat(guardHealth.status()).isEqualTo(BuzhouHealth.Status.UNKNOWN);
                    assertThat(guardHealth.details()).containsEntry("auditEnabled", false);
                });
    }
}
