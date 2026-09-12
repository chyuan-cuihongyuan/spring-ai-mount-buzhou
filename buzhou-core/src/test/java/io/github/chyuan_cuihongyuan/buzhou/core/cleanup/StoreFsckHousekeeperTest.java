package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 538 / T827–828：fsck 定时巡检——干净 store findings 0、孤儿摘要
 * 巡出 findings、生命周期起停、yml enabled 装配/缺席。
 */
class StoreFsckHousekeeperTest {

    @Test
    void evaluateOnceOnCleanStoresFindsNothing() {
        StoreFsckHousekeeper keeper = new StoreFsckHousekeeper(
                Buzhou.inMemoryStores(), null, Duration.ofHours(1));
        var report = keeper.evaluateOnce();
        assertThat(report.findings()).isEmpty();
        assertThat(keeper.runs()).isEqualTo(1);
        assertThat(keeper.totalFindings()).isZero();
    }

    @Test
    void orphanSummaryIsDetected() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        // 孤儿摘要：有摘要无消息
        stores.summaryStore().save("sess-orphan",
                new io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary(
                        "sess-orphan", 1, java.util.Map.of("facts", "x"), 5, java.time.Instant.now()));
        // fsck 以观测 span 判定会话存在——孤儿 = 有摘要+有观测、无消息
        stores.observabilityStore().saveSpans(java.util.List.of(
                new io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord(
                        "sp-orphan", null, "sess-orphan", 1, "SESSION", "session",
                        java.time.Instant.now(), java.time.Instant.now(), "OK", java.util.Map.of())));
        StoreFsckHousekeeper keeper = new StoreFsckHousekeeper(
                stores, null, Duration.ofHours(1));
        var report = keeper.evaluateOnce();
        assertThat(report.findings()).isNotEmpty();
        assertThat(keeper.totalFindings()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void lifecycleStartStopTransitions() {
        StoreFsckHousekeeper keeper = new StoreFsckHousekeeper(
                Buzhou.inMemoryStores(), null, Duration.ofHours(1));
        assertThat(keeper.isRunning()).isFalse();
        keeper.start();
        assertThat(keeper.isRunning()).isTrue();
        keeper.stop();
        assertThat(keeper.isRunning()).isFalse();
    }

    @Test
    void ymlAssemblyOnlyWhenEnabled() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withBean(io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores.class,
                        () -> io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores())
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues("buzhou.fsck.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouStoreFsckHousekeeper");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withBean(io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores.class,
                        () -> io.github.chyuan_cuihongyuan.buzhou.core.Buzhou.inMemoryStores())
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouStoreFsckHousekeeper");
                });
    }
}
