package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4014 / T6030：Bitcask 键目录合同——追加覆盖死账、合并门、
 * 合并计划与执行、删除死账、畸形 fail-fast。
 */
class BitcaskKeydirTest {

    @Test
    void overwriteShouldAccumulateDeadBytes() {
        BitcaskKeydir store = new BitcaskKeydir();
        assertThat(store.append("k", 100)).isZero();   // 首写偏移 0
        assertThat(store.append("other", 50)).isEqualTo(100);
        assertThat(store.append("k", 200)).isEqualTo(150);   // 覆盖旧版
        assertThat(store.totalBytes()).isEqualTo(350);
        assertThat(store.deadBytes()).isEqualTo(100);   // 旧 k 版本入死账
        assertThat(store.deadRatio()).isEqualTo(100.0 / 350);
        assertThat(store.contains("k")).isTrue();
        assertThat(store.offsetOf("k")).isEqualTo(150);   // 最新偏移
        assertThat(store.offsetOf("missing")).isEqualTo(-1);
    }

    @Test
    void mergeGateAndExecutionShouldReclaim() {
        BitcaskKeydir store = new BitcaskKeydir();
        store.append("k", 100);
        store.append("other", 50);
        store.append("k", 200);
        assertThat(store.shouldMerge(0.2)).isTrue();   // 死比 100/350≈0.29
        assertThat(store.shouldMerge(0.5)).isFalse();
        BitcaskKeydir.MergePlan plan = store.mergePlan();
        assertThat(plan.liveKeys()).isEqualTo(2);
        assertThat(plan.liveBytes()).isEqualTo(250);
        store.applyMerge();
        assertThat(store.deadBytes()).isZero();
        assertThat(store.totalBytes()).isEqualTo(250);   // 压实后=活字节
        assertThat(store.contains("k")).isTrue();   // 键目录不动
        assertThat(store.contains("other")).isTrue();
        assertThat(store.deadRatio()).isZero();
    }

    @Test
    void deleteShouldMoveBytesToDead() {
        BitcaskKeydir store = new BitcaskKeydir();
        store.append("k", 100);
        store.append("other", 50);
        store.append("k", 200);
        store.delete("other");
        assertThat(store.contains("other")).isFalse();
        assertThat(store.deadBytes()).isEqualTo(150);   // 旧 k + other
        assertThat(store.mergePlan().liveKeys()).isEqualTo(1);
        assertThat(store.mergePlan().liveBytes()).isEqualTo(200);
        store.applyMerge();
        assertThat(store.totalBytes()).isEqualTo(200);
    }

    @Test
    void emptyStoreShouldBeHonest() {
        BitcaskKeydir store = new BitcaskKeydir();
        assertThat(store.totalBytes()).isZero();
        assertThat(store.deadRatio()).isNaN();
        assertThat(store.mergePlan().liveKeys()).isZero();
        assertThat(store.shouldMerge(0.0)).isFalse();   // NaN ≥ 0 为假——空无可收
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        BitcaskKeydir store = new BitcaskKeydir();
        assertThatThrownBy(() -> store.append(null, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.append("k", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.delete(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.shouldMerge(1.5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
