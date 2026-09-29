package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LfuEvictionTest {

    @Test
    void shouldEvictLowestFrequencyOldestFirst() {
        LfuEviction cache = new LfuEviction(3);
        assertThat(cache.put("A", "1")).isNull();
        assertThat(cache.put("B", "2")).isNull();
        assertThat(cache.put("C", "3")).isNull();
        assertThat(cache.put("A", "1b")).isNull();
        assertThat(cache.get("A")).isEqualTo("1b");
        assertThat(cache.put("D", "4")).isEqualTo("B");
        assertThat(cache.peek("B")).isNull();
        assertThat(cache.peek("C")).isEqualTo("3");
        assertThat(cache.peek("A")).isEqualTo("1b");
        assertThat(cache.size()).isEqualTo(3);
    }

    @Test
    void shouldUpsertWithoutEvictionAndMatchBruteOracleOnRandomOps() {
        LfuEviction cache = new LfuEviction(3);
        assertThat(cache.put("A", "1")).isNull();
        assertThat(cache.put("A", "2")).isNull();
        assertThat(cache.size()).isEqualTo(1);
        assertThat(cache.put("B", "3")).isNull();
        assertThat(cache.put("C", "4")).isNull();
        assertThat(cache.put("D", "5")).isEqualTo("B");

        Random random = new Random(8021);
        LfuEviction cache2 = new LfuEviction(3);
        Map<String, OracleEntry> oracle = new HashMap<>();
        for (int round = 0; round < 500; round++) {
            String key = "k" + random.nextInt(6);
            if (random.nextInt(3) == 0) {
                String got = cache2.get(key);
                OracleEntry entry = oracle.get(key);
                String expected = entry == null ? null : entry.value();
                assertThat(got).as("round=%d get=%s", round, key).isEqualTo(expected);
                if (entry != null) {
                    oracle.put(key, new OracleEntry(entry.value(), entry.freq() + 1, round));
                }
            } else {
                String value = "v" + round;
                String evicted = cache2.put(key, value);
                String expectedEvicted = null;
                if (!oracle.containsKey(key) && oracle.size() == 3) {
                    long bestFreq = Long.MAX_VALUE;
                    long bestSeq = Long.MAX_VALUE;
                    for (Map.Entry<String, OracleEntry> entry : oracle.entrySet()) {
                        long freq = entry.getValue().freq();
                        long seq = entry.getValue().seq();
                        if (freq < bestFreq || (freq == bestFreq && seq < bestSeq)) {
                            bestFreq = freq;
                            bestSeq = seq;
                            expectedEvicted = entry.getKey();
                        }
                    }
                }
                assertThat(evicted).as("round=%d put=%s", round, key).isEqualTo(expectedEvicted);
                if (expectedEvicted != null) {
                    oracle.remove(expectedEvicted);
                }
                long seq = round;
                long freq = oracle.containsKey(key) ? oracle.get(key).freq() + 1 : 1;
                oracle.put(key, new OracleEntry(value, freq, seq));
            }
            assertThat(cache2.size()).isEqualTo(oracle.size());
        }
    }

    @Test
    void shouldFailFastOnBadCapacityAndNulls() {
        assertThatThrownBy(() -> new LfuEviction(0)).isInstanceOf(IllegalArgumentException.class);
        LfuEviction cache = new LfuEviction(2);
        assertThatThrownBy(() -> cache.put(null, "v")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.put("k", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.get(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.peek(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(cache.get("absent")).isNull();
        assertThat(cache.peek("absent")).isNull();
    }
    private record OracleEntry(String value, long freq, long seq) {
    }
}
