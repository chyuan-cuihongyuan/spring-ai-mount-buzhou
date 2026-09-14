package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1058 / impl 810：工具集轮询提供器读面——双入口（轮询 / 写后直调）共同汇入
 * 三结局桶（变更检出/无变更/失败），守恒 polls + pushRefreshes = 三桶和，归零。
 * 经 InMemoryToolSetSpecStore 写后 listener 驱动直调路径，不依赖真实轮询周期。
 */
class ToolSetPollStatsTest {

    @BeforeEach
    void reset() {
        DbToolSetProvider.resetForTest();
    }

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STDIO, "npx -y @" + name,
                java.util.Map.of(), null, null, java.util.Set.of());
    }

    @Test
    void pushRefreshWithChangeCountsItsBucket() {
        InMemoryToolSetSpecStore store = new InMemoryToolSetSpecStore();
        DbToolSetProvider provider = new DbToolSetProvider(store, Duration.ofHours(1));
        try {
            store.replaceAll(List.of(spec("github"))); // 写后 listener 直调
            assertThat(provider.currentToolSets()).hasSize(1);
        } finally {
            provider.close();
        }

        DbToolSetProvider.ToolSetPollStats stats = DbToolSetProvider.stats();
        assertThat(stats.pushRefreshes()).isEqualTo(1);
        assertThat(stats.changesDetected()).isEqualTo(1);
        assertThat(stats.polls()).isZero();
    }

    @Test
    void unchangedRefreshCountsItsBucket() {
        InMemoryToolSetSpecStore store = new InMemoryToolSetSpecStore();
        DbToolSetProvider provider = new DbToolSetProvider(store, Duration.ofHours(1));
        try {
            store.replaceAll(List.of(spec("a"))); // 变更轮
            store.replaceAll(List.of(spec("a"))); // 同清单 equals 相等 → 无变更轮
        } finally {
            provider.close();
        }

        DbToolSetProvider.ToolSetPollStats stats = DbToolSetProvider.stats();
        assertThat(stats.unchangedRefreshes()).isEqualTo(1);
        assertThat(stats.changesDetected()).isEqualTo(1);
    }

    @Test
    void failingLoadCountsItsBucketAndStillPropagates() {
        // 匿名子类覆写 loadAll 抛错：直调路径失败入桶后异常照旧外溢（行为逐位不变）
        InMemoryToolSetSpecStore broken = new InMemoryToolSetSpecStore() {
            @Override
            public List<ToolSetSpec> loadAll() {
                throw new IllegalStateException("db down");
            }
        };
        DbToolSetProvider provider = new DbToolSetProvider(broken, Duration.ofHours(1));
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> broken.replaceAll(List.of(spec("a"))))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            provider.close();
        }

        DbToolSetProvider.ToolSetPollStats stats = DbToolSetProvider.stats();
        assertThat(stats.refreshFailures()).isEqualTo(1);
        assertThat(stats.changesDetected()).isZero();
    }

    @Test
    void conservationIdentityHoldsAcrossMixedRefreshes() {
        InMemoryToolSetSpecStore store = new InMemoryToolSetSpecStore();
        DbToolSetProvider provider = new DbToolSetProvider(store, Duration.ofHours(1));
        try {
            store.replaceAll(List.of(spec("a")));            // changesDetected（初装）
            store.replaceAll(List.of(spec("a")));            // unchangedRefreshes
            store.replaceAll(List.of(spec("a"), spec("b"))); // changesDetected
        } finally {
            provider.close();
        }

        DbToolSetProvider.ToolSetPollStats stats = DbToolSetProvider.stats();
        assertThat(stats.pushRefreshes()).isEqualTo(3);
        assertThat(stats.polls() + stats.pushRefreshes())
                .isEqualTo(stats.changesDetected() + stats.unchangedRefreshes()
                        + stats.refreshFailures());
        assertThat(stats.changesDetected()).isEqualTo(2);
        assertThat(stats.unchangedRefreshes()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        InMemoryToolSetSpecStore store = new InMemoryToolSetSpecStore();
        DbToolSetProvider provider = new DbToolSetProvider(store, Duration.ofHours(1));
        try {
            store.replaceAll(List.of(spec("a")));
        } finally {
            provider.close();
        }
        assertThat(DbToolSetProvider.stats().pushRefreshes()).isEqualTo(1);

        DbToolSetProvider.resetForTest();

        DbToolSetProvider.ToolSetPollStats stats = DbToolSetProvider.stats();
        assertThat(stats.polls()).isZero();
        assertThat(stats.pushRefreshes()).isZero();
        assertThat(stats.changesDetected()).isZero();
    }
}
