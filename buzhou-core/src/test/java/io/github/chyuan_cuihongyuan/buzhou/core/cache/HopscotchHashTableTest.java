package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HopscotchHashTableTest {

    @Test
    void shouldPutGetOverwriteAndRemove() {
        HopscotchHashTable table = new HopscotchHashTable(8, 16);
        assertThat(table.put("a", "1")).isNull();
        assertThat(table.put("b", "2")).isNull();
        assertThat(table.get("a")).isEqualTo("1");
        assertThat(table.get("c")).isNull();
        assertThat(table.put("a", "9")).isEqualTo("1");
        assertThat(table.get("a")).isEqualTo("9");
        assertThat(table.size()).isEqualTo(2);
        table.remove("a");
        assertThat(table.get("a")).isNull();
        assertThat(table.size()).isEqualTo(1);
        assertThat(table.put("a", "3")).isNull();
        assertThat(table.get("a")).isEqualTo("3");
        assertThat(table.neighborhoodInvariantHolds()).isTrue();
    }

    @Test
    void shouldMatchHashMapOracleAndHoldInvariantUnderChurn() {
        Random random = new Random(8012);
        HopscotchHashTable table = new HopscotchHashTable(4, 16);
        Map<String, String> oracle = new HashMap<>();
        for (int round = 0; round < 1000; round++) {
            String key = "k" + random.nextInt(120);
            if (random.nextInt(4) == 0 && !oracle.isEmpty()) {
                String victim = "k" + random.nextInt(120);
                if (oracle.containsKey(victim)) {
                    oracle.remove(victim);
                    table.remove(victim);
                }
            } else {
                String value = "v" + round;
                String old = oracle.put(key, value);
                assertThat(table.put(key, value)).isEqualTo(old);
            }
            assertThat(table.size()).isEqualTo(oracle.size());
            if (round % 50 == 0) {
                assertThat(table.neighborhoodInvariantHolds()).isTrue();
            }
        }
        assertThat(table.neighborhoodInvariantHolds()).isTrue();
        for (Map.Entry<String, String> entry : oracle.entrySet()) {
            assertThat(table.get(entry.getKey())).isEqualTo(entry.getValue());
        }
    }

    @Test
    void shouldExpandAndFailFast() {
        HopscotchHashTable small = new HopscotchHashTable(4, 8);
        int before = small.bucketCount();
        for (int i = 0; i < 60; i++) {
            small.put("key-" + i, "v" + i);
        }
        assertThat(small.bucketCount()).isGreaterThan(before);
        assertThat(small.size()).isEqualTo(60);
        for (int i = 0; i < 60; i++) {
            assertThat(small.get("key-" + i)).isEqualTo("v" + i);
        }
        assertThat(small.neighborhoodInvariantHolds()).isTrue();

        assertThatThrownBy(() -> new HopscotchHashTable(1, 8))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HopscotchHashTable(33, 8))
                .isInstanceOf(IllegalArgumentException.class);
        HopscotchHashTable table = new HopscotchHashTable();
        assertThatThrownBy(() -> table.put(null, "v")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.put("k", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.get(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.remove("absent")).isInstanceOf(IllegalArgumentException.class);
    }
}
