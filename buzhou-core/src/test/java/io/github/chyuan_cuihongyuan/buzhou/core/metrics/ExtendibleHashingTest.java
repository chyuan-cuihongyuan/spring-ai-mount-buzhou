package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 6032：ExtendibleHashing 合同——目录深度按需翻倍。
 * 大容量插入全命中；目录翻倍守恒；重复幂等；结构读数。
 */
class ExtendibleHashingTest {

    @Test
    void largeInsertAllContained() {
        ExtendibleHashing table = new ExtendibleHashing();
        for (long key = 0; key < 300; key++) {
            table.insert(key * 7);
        }
        for (long key = 0; key < 300; key++) {
            assertThat(table.contains(key * 7)).as("key %d", key * 7).isTrue();
        }
        assertThat(table.contains(1)).isFalse();
        assertThat(table.size()).isEqualTo(300);
        assertThat(table.globalDepth()).isGreaterThanOrEqualTo(3);
        assertThat(table.directorySize()).isEqualTo(1 << table.globalDepth());
        assertThat(table.bucketCount()).isLessThanOrEqualTo(table.directorySize());
    }

    @Test
    void duplicateInsertIsIdempotent() {
        ExtendibleHashing table = new ExtendibleHashing();
        table.insert(42);
        table.insert(42);
        table.insert(42);
        assertThat(table.size()).isEqualTo(1);
        assertThat(table.contains(42)).isTrue();
    }

    @Test
    void directoryDoublesOnDemand() {
        ExtendibleHashing table = new ExtendibleHashing();
        int depthBefore = table.globalDepth();
        for (long key = 0; key < 100; key++) {
            table.insert(key);
            assertThat(table.globalDepth()).as("depth 单调不减").isGreaterThanOrEqualTo(depthBefore);
            depthBefore = Math.max(depthBefore, table.globalDepth());
        }
        assertThat(table.globalDepth()).isGreaterThan(1);
        assertThat(table.bucketCount()).isGreaterThan(2);
    }

    @Test
    void initialShape() {
        ExtendibleHashing table = new ExtendibleHashing();
        assertThat(table.globalDepth()).isEqualTo(1);
        assertThat(table.directorySize()).isEqualTo(2);
        assertThat(table.size()).isZero();
    }
}
