package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6037：StableBloomFilter 合同——流式衰减位阵。
 * 近期插入恒真（无假阴性）；停插后旧元素淡出；确定性；
 * fail-fast。
 */
class StableBloomFilterTest {

    @Test
    void recentInsertsAlwaysTrue() {
        StableBloomFilter filter = new StableBloomFilter(4096, 2, 3);
        for (int i = 0; i < 1000; i++) {
            filter.insert("recent-" + i);
            assertThat(filter.mightContain("recent-" + i))
                    .as("插入即真 %d", i).isTrue();
        }
    }

    @Test
    void oldElementsFadeOutWithoutInserts() {
        StableBloomFilter filter = new StableBloomFilter(4096, 3, 3);
        filter.insert("old-member");
        for (int i = 0; i < 20000; i++) {
            filter.insert("stream-" + i);
        }
        assertThat(filter.mightContain("old-member"))
                .as("长期不重现的旧成员被衰减淡出").isFalse();
    }

    @Test
    void repeatedInsertStaysVisible() {
        StableBloomFilter filter = new StableBloomFilter(1024, 2, 3);
        for (int round = 0; round < 10; round++) {
            filter.insert("persistent");
            for (int noise = 0; noise < 50; noise++) {
                filter.insert("noise-" + noise);
            }
            assertThat(filter.mightContain("persistent")).isTrue();
        }
    }

    @Test
    void deterministicBehavior() {
        StableBloomFilter a = new StableBloomFilter(2048, 2, 3);
        StableBloomFilter b = new StableBloomFilter(2048, 2, 3);
        for (int i = 0; i < 100; i++) {
            a.insert("key-" + i);
            b.insert("key-" + i);
        }
        for (int i = 0; i < 100; i++) {
            assertThat(b.mightContain("key-" + i))
                    .isEqualTo(a.mightContain("key-" + i));
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new StableBloomFilter(0, 1, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StableBloomFilter(16, 17, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StableBloomFilter(16, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        StableBloomFilter filter = new StableBloomFilter(64, 1, 3);
        assertThatThrownBy(() -> filter.insert(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> filter.mightContain(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
