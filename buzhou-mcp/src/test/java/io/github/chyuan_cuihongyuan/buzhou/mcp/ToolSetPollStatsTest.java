package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1058 / impl 810：工具集轮询提供器读面——变更检出/无变更/轮询失败三桶守恒、
 * resetForTest 归零。经 InMemoryToolSetSpecStore 的写后 listener 驱动 checkQuietly
 * 同点（构造即注册），不依赖真实轮询周期。
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
    void changeDetectionCountsItsBucket() {
        InMemoryToolSetSpecStore store = new InMemoryToolSetSpecStore();
        DbToolSetProvider provider = new DbToolSetProvider(store, Duration.ofHours(1));
        try {
            store.replaceAll(List.of(spec("github"))); // 写后通知 listener → checkQuietly
            assertThat(provider.currentToolSets()).hasSize(1);
        } finally {
            provider.close();
        }

        DbToolSetProvider.ToolSetPollStats stats = DbToolSetProvider.stats();
        assertThat(stats.polls()).isEqualTo(1);
        assertThat(stats.changesDetected()).isEqualTo(1);
        assertThat(stats.unchangedPolls()).isZero();
        assertThat(stats.pollFailures()).isZero();
    }

    @Test
    void unchangedPollCountsItsBucket() {
        InMemoryToolSetSpecStore store = new InMemoryToolSetSpecStore();
        DbToolSetProvider provider = new DbToolSetProvider(store, Duration.ofHours(1));
        try {
            store.replaceAll(List.of(spec("a"))); // 变更轮
            store.replaceAll(List.of(spec("a"))); // 同清单 equals 相等 → 无变更轮
        } finally {
            provider.close();
        }

        DbToolSetProvider.ToolSetPollStats stats = DbToolSetProvider.stats();
        assertThat(stats.unchangedPolls()).isEqualTo(1);
        assertThat(stats.changesDetected()).isEqualTo(1);
    }

    @Test
    void failingLoadCountsItsBucket() {
        // 匿名子类覆写 loadAll 抛错：listener 触发轮询 → loadAll 抛 → pollFailures
        InMemoryToolSetSpecStore broken = new InMemoryToolSetSpecStore() {
            @Override
            public List<ToolSetSpec> loadAll() {
                throw new IllegalStateException("db down");
            }
        };
        DbToolSetProvider provider = new DbToolSetProvider(broken, Duration.ofHours(1));
        try {
            broken.replaceAll(List.of(spec("a")));
        } finally {
            provider.close();
        }

        DbToolSetProvider.ToolSetPollStats stats = DbToolSetProvider.stats();
        assertThat(stats.pollFailures()).isEqualTo(1);
        assertThat(stats.changesDetected()).isZero();
    }

    @Test
    void conservationIdentityHoldsAcrossMixedPolls() {
        InMemoryToolSetSpecStore store = new InMemoryToolSetSpecStore();
        DbToolSetProvider provider = new DbToolSetProvider(store, Duration.ofHours(1));
        try {
            store.replaceAll(List.of(spec("a")));             // changesDetected（初装）
            store.replaceAll(List.of(spec("a")));             // unchangedPolls
            store.replaceAll(List.of(spec("a"), spec("b")));  // changesDetected
        } finally {
            provider.close();
        }

        DbToolSetProvider.ToolSetPollStats stats = DbToolSetProvider.stats();
        assertThat(stats.polls()).isEqualTo(3);
        assertThat(stats.polls())
                .isEqualTo(stats.changesDetected() + stats.unchangedPolls() + stats.pollFailures());
        assertThat(stats.changesDetected()).isEqualTo(2);
        assertThat(stats.unchangedPolls()).isEqualTo(1);
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
        assertThat(DbToolSetProvider.stats().polls()).isEqualTo(1);

        DbToolSetProvider.resetForTest();

        DbToolSetProvider.ToolSetPollStats stats = DbToolSetProvider.stats();
        assertThat(stats.polls()).isZero();
        assertThat(stats.changesDetected()).isZero();
    }
}
