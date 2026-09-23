package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5025 / T6152：跳表合同——圣像对拍（固定种子 500 操作）、
 * 同种子重放、upsert、null fail-fast。
 */
class SkipListTest {

    @Test
    void randomOperationsShouldMatchTreeMapOracle() {
        SkipList skipList = new SkipList(5025L);
        TreeMap<String, String> oracle = new TreeMap<>();
        Random random = new Random(7L);
        for (int op = 0; op < 500; op++) {
            String key = "k" + random.nextInt(120);
            String value = "v" + op;
            skipList.put(key, value);
            oracle.put(key, value);
        }
        assertThat(skipList.size()).isEqualTo(oracle.size());
        assertThat(skipList.keysInOrder()).isEqualTo(List.copyOf(oracle.keySet()));
        for (String key : oracle.keySet()) {
            assertThat(skipList.get(key)).isEqualTo(oracle.get(key));
        }
    }

    @Test
    void sameSeedShouldReplaySameStructure() {
        SkipList first = new SkipList(99L);
        SkipList second = new SkipList(99L);
        Random random = new Random(1L);
        for (int i = 0; i < 80; i++) {
            String key = "k" + random.nextInt(60);
            first.put(key, "v" + i);
            second.put(key, "v" + i);
        }
        assertThat(first.keysInOrder()).isEqualTo(second.keysInOrder());
    }

    @Test
    void putShouldUpsertExistingKey() {
        SkipList skipList = new SkipList(5L);
        skipList.put("k", "v1");
        skipList.put("k", "v2");
        assertThat(skipList.size()).isEqualTo(1);
        assertThat(skipList.get("k")).isEqualTo("v2");
    }

    @Test
    void nullKeyOrValueShouldFailFast() {
        SkipList skipList = new SkipList(1L);
        assertThatThrownBy(() -> skipList.put(null, "v")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> skipList.put("k", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> skipList.get(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
