package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1418 / T2138：Redis 慢操作榜——阈值严格大于入榜、FIFO 挤旧、
 * entries 新→旧、totalSlowOps 水位、阈值动态调整、reset 复位；
 * 静态面测试前后归零防串扰。
 */
class RedisSlowOpLogTest {

    @BeforeEach
    void reset() {
        RedisSlowOpLog.resetForTest();
    }

    @AfterEach
    void resetAfter() {
        RedisSlowOpLog.resetForTest();
    }

    @Test
    void belowThresholdIsIgnored() {
        RedisSlowOpLog.record("append", 10);
        assertThat(RedisSlowOpLog.entries()).isEmpty();
        assertThat(RedisSlowOpLog.totalSlowOps()).isZero();
    }

    @Test
    void strictlyAboveThresholdEntersLog() {
        RedisSlowOpLog.record("load", 100); // 恰等于阈值——严格大于才入榜
        assertThat(RedisSlowOpLog.entries()).isEmpty();
        RedisSlowOpLog.record("load", 101);
        assertThat(RedisSlowOpLog.entries()).hasSize(1);
        assertThat(RedisSlowOpLog.entries().get(0).op()).isEqualTo("load");
        assertThat(RedisSlowOpLog.entries().get(0).durationMillis()).isEqualTo(101);
    }

    @Test
    void entriesAreNewestFirstWithBoundedFifo() {
        for (int i = 0; i < RedisSlowOpLog.CAPACITY + 5; i++) {
            RedisSlowOpLog.record("op" + i, 1000 + i);
        }
        List<RedisSlowOpLog.Entry> entries = RedisSlowOpLog.entries();
        // 环有界：榜内只剩最近 CAPACITY 条
        assertThat(entries).hasSize(RedisSlowOpLog.CAPACITY);
        // 新→旧：最新一条在最前（CAPACITY+4），最旧被挤出（op0–op4 共 5 条出环）
        assertThat(entries.get(0).op()).isEqualTo("op" + (RedisSlowOpLog.CAPACITY + 4));
        assertThat(entries.get(entries.size() - 1).op()).isEqualTo("op5");
        // 水位：挤出也累计
        assertThat(RedisSlowOpLog.totalSlowOps()).isEqualTo(RedisSlowOpLog.CAPACITY + 5);
    }

    @Test
    void thresholdIsDynamicallyAdjustable() {
        long old = RedisSlowOpLog.configureThresholdMillis(50);
        assertThat(old).isEqualTo(RedisSlowOpLog.DEFAULT_THRESHOLD_MILLIS);
        RedisSlowOpLog.record("findById", 60);
        assertThat(RedisSlowOpLog.entries()).hasSize(1); // 50 阈值下 60ms 入榜
        RedisSlowOpLog.configureThresholdMillis(5000);
        RedisSlowOpLog.record("findById", 60);
        assertThat(RedisSlowOpLog.entries()).hasSize(1); // 新阈值下不再入榜
    }

    @Test
    void resetClearsLogAndTotalAndThreshold() {
        RedisSlowOpLog.configureThresholdMillis(10);
        RedisSlowOpLog.record("append", 11);
        RedisSlowOpLog.resetForTest();
        assertThat(RedisSlowOpLog.entries()).isEmpty();
        assertThat(RedisSlowOpLog.totalSlowOps()).isZero();
        // 阈值复位：11ms 在默认 100ms 下不再入榜
        RedisSlowOpLog.record("append", 11);
        assertThat(RedisSlowOpLog.entries()).isEmpty();
    }
}
