package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import java.util.List;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.KeyCompaction.CompactionResult;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.KeyCompaction.Entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2014 / T3130：键压缩合同——最新 seq 胜、乱序幂等、tombstone
 * 删除、墓碑后复活、对账计数、畸形 fail-fast。
 */
class KeyCompactionTest {

    @Test
    void latestSequenceShouldWinPerKey() {
        CompactionResult result = KeyCompaction.compact(List.of(
                new Entry("tool:rm", 1, "v1"),
                new Entry("tool:rm", 2, "v2"),
                new Entry("tool:ls", 1, "only")));
        assertThat(result.latest()).containsEntry("tool:rm", "v2");
        assertThat(result.latest()).containsEntry("tool:ls", "only");
        assertThat(result.superseded()).isEqualTo(1);
    }

    @Test
    void compactionShouldBeOrderInsensitive() {
        // 乱序输入（旧值后到）同结果——压缩幂等（Kafka 语义核心）
        List<Entry> ordered = List.of(
                new Entry("k", 1, "old"), new Entry("k", 2, "new"));
        List<Entry> reversed = List.of(
                new Entry("k", 2, "new"), new Entry("k", 1, "old"));
        assertThat(KeyCompaction.compact(ordered).latest())
                .isEqualTo(KeyCompaction.compact(reversed).latest());
        assertThat(KeyCompaction.compact(ordered).latest()).containsEntry("k", "new");
    }

    @Test
    void tombstoneShouldDeleteKey() {
        CompactionResult result = KeyCompaction.compact(List.of(
                new Entry("k", 1, "v1"),
                new Entry("k", 2, null))); // 墓碑
        assertThat(result.latest()).doesNotContainKey("k");
        assertThat(result.tombstones()).isEqualTo(1);
    }

    @Test
    void writeAfterTombstoneShouldResurrect() {
        CompactionResult result = KeyCompaction.compact(List.of(
                new Entry("k", 1, "v1"),
                new Entry("k", 2, null),   // 墓碑
                new Entry("k", 3, "v3"))); // 复活
        assertThat(result.latest()).containsEntry("k", "v3");
        assertThat(result.tombstones()).isZero(); // 最大 seq 非墓碑
    }

    @Test
    void staleTombstoneShouldNotDeleteNewerValue() {
        CompactionResult result = KeyCompaction.compact(List.of(
                new Entry("k", 1, null),   // 旧墓碑
                new Entry("k", 2, "v2"))); // 新值
        assertThat(result.latest()).containsEntry("k", "v2"); // 旧墓碑失效
    }

    @Test
    void equalSequenceShouldTakeLatter() {
        CompactionResult result = KeyCompaction.compact(List.of(
                new Entry("k", 5, "first"),
                new Entry("k", 5, "second"))); // 同 seq 后见者（退化口径）
        assertThat(result.latest()).containsEntry("k", "second");
    }

    @Test
    void compactionRatioShouldAccount() {
        CompactionResult result = KeyCompaction.compact(List.of(
                new Entry("a", 1, "v1"), new Entry("a", 2, "v2"),
                new Entry("b", 1, "v1")));
        assertThat(result.superseded()).isEqualTo(1);
        assertThat(result.compactionRatio(3)).isCloseTo(1.0 / 3.0,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(KeyCompaction.compact(List.of()).compactionRatio(0)).isZero(); // 空不除零
    }

    @Test
    void nullCollectionShouldCompactToEmpty() {
        assertThat(KeyCompaction.compact(null).latest()).isEmpty();
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> KeyCompaction.compact(
                List.of(new Entry(null, 1, "v"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KeyCompaction.compact(
                List.of(new Entry("k", -1, "v"))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
