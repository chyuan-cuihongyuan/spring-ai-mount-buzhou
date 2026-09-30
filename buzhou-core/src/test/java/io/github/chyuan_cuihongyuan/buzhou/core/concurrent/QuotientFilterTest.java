package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QuotientFilter 契约测试（spec 10002 / X10006）：无误报否定圣像 +
 * 误报率上界 + 簇密集移位压力 + 集合真值对拍 + 满容 fail-fast。
 */
class QuotientFilterTest {

    @Test
    void shouldFindAllAddedKeys() {
        QuotientFilter filter = new QuotientFilter(12, 10);
        for (long key = 0; key < 1500; key++) {
            assertThat(filter.add(key * 7919L)).as("key %d 加入", key).isTrue();
        }
        for (long key = 0; key < 1500; key++) {
            assertThat(filter.mightContain(key * 7919L)).as("key %d 必命中", key).isTrue();
        }
        assertThat(filter.size()).isEqualTo(1500);
    }

    @Test
    void shouldBoundFalsePositiveRate() {
        QuotientFilter filter = new QuotientFilter(14, 12);
        for (long key = 0; key < 3000; key++) {
            filter.add(key * 65537L);
        }
        int falsePositives = 0;
        for (long key = 0; key < 100_000; key++) {
            long probe = 1_000_000_000L + key * 40503L;
            if (filter.mightContain(probe)) {
                falsePositives++;
            }
        }
        assertThat((double) falsePositives / 100_000).isLessThan(0.02);
    }

    @Test
    void shouldRejectDuplicateAdd() {
        QuotientFilter filter = new QuotientFilter(10, 10);
        assertThat(filter.add(42L)).isTrue();
        assertThat(filter.add(42L)).isFalse();
        assertThat(filter.size()).isEqualTo(1);
    }

    @Test
    void shouldSurviveHighLoadClusterStress() {
        QuotientFilter filter = new QuotientFilter(10, 8);
        Random random = new Random(20260930L);
        Set<Long> truth = new HashSet<>();
        while (truth.size() < 350) {
            long key = random.nextInt(50_000);
            if (truth.add(key)) {
                filter.add(key);
            }
        }
        for (long key : truth) {
            assertThat(filter.mightContain(key)).as("高载 key %d 必命中", key).isTrue();
        }
    }

    @Test
    void shouldAgreeWithSetGroundTruthOneSided() {
        QuotientFilter filter = new QuotientFilter(12, 9);
        Set<Long> truth = new HashSet<>();
        Random random = new Random(42L);
        for (int i = 0; i < 1200; i++) {
            long key = random.nextInt(100_000);
            if (filter.add(key)) {
                truth.add(key);
            } else {
                assertThat(truth.contains(key)).as("重复加 key %d 真值必在", key).isTrue();
            }
        }
        for (int i = 0; i < 10_000; i++) {
            long probe = random.nextInt(100_000);
            if (!filter.mightContain(probe)) {
                assertThat(truth.contains(probe)).as("否定即必不在（key %d）", probe).isFalse();
            }
        }
    }

    @Test
    void shouldFailFastWhenFull() {
        QuotientFilter filter = new QuotientFilter(4, 4);
        boolean overflowed = false;
        try {
            for (long key = 0; key < 1000; key++) {
                filter.add(key);
            }
        } catch (IllegalStateException expected) {
            overflowed = true;
        }
        assertThat(overflowed).as("16 槽装 1000 键必满容").isTrue();
    }

    @Test
    void shouldFailFastOnBadConstructorArgs() {
        assertThatThrownBy(() -> new QuotientFilter(0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QuotientFilter(17, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QuotientFilter(10, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QuotientFilter(16, 16))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldBeDeterministicAcrossRuns() {
        QuotientFilter first = new QuotientFilter(12, 10);
        QuotientFilter second = new QuotientFilter(12, 10);
        for (long key = 0; key < 2000; key += 7) {
            first.add(key);
            second.add(key);
        }
        assertThat(first.size()).isEqualTo(second.size());
        for (long key = 0; key < 2000; key += 7) {
            assertThat(first.mightContain(key)).isEqualTo(second.mightContain(key));
        }
    }
}
