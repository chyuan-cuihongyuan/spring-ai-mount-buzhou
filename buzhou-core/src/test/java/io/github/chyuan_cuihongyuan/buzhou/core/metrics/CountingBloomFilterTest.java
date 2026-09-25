package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6036：CountingBloomFilter 合同——可删除计数布隆。
 * 无假阴性；删除后不再报告；重复插入计数守恒；假阳性率
 * 经验受控；fail-fast。
 */
class CountingBloomFilterTest {

    @Test
    void noFalseNegativesWithRemoval() {
        CountingBloomFilter filter = new CountingBloomFilter(1000, 0.01, 4);
        for (int i = 0; i < 500; i++) {
            filter.insert("item-" + i);
        }
        for (int i = 0; i < 500; i++) {
            assertThat(filter.mightContain("item-" + i)).as("无假阴性 %d", i).isTrue();
        }
        filter.remove("item-7");
        filter.insert("item-7");
        assertThat(filter.mightContain("item-7")).isTrue();
    }

    @Test
    void removalMakesElementAbsent() {
        CountingBloomFilter filter = new CountingBloomFilter(100, 0.01, 3);
        filter.insert("solo");
        assertThat(filter.mightContain("solo")).isTrue();
        filter.remove("solo");
        assertThat(filter.mightContain("solo")).as("计数回零即缺席").isFalse();
        assertThat(filter.insertedCount()).isZero();
    }

    @Test
    void duplicateInsertsRequireEqualRemovals() {
        CountingBloomFilter filter = new CountingBloomFilter(50, 0.01, 3);
        filter.insert("dup");
        filter.insert("dup");
        filter.remove("dup");
        assertThat(filter.mightContain("dup")).as("计数 1 仍报告存在").isTrue();
        filter.remove("dup");
        assertThat(filter.mightContain("dup")).isFalse();
    }

    @Test
    void falsePositiveRateBoundedEmpirically() {
        CountingBloomFilter filter = new CountingBloomFilter(1000, 0.01, 5);
        for (int i = 0; i < 1000; i++) {
            filter.insert("present-" + i);
        }
        Random rng = new Random(6035L);
        int falsePositives = 0;
        int probes = 2000;
        for (int i = 0; i < probes; i++) {
            if (filter.mightContain("absent-" + rng.nextLong())) {
                falsePositives++;
            }
        }
        assertThat(falsePositives).as("假阳性 ≤ 5%（目标 1%）").isLessThanOrEqualTo(probes / 20);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new CountingBloomFilter(0, 0.01, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CountingBloomFilter(100, 1.0, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CountingBloomFilter(100, 0.01, 0))
                .isInstanceOf(IllegalArgumentException.class);
        CountingBloomFilter filter = new CountingBloomFilter(10, 0.01, 3);
        assertThatThrownBy(() -> filter.insert(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> filter.remove(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
