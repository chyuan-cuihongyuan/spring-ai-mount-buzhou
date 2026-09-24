package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5039 / T6180：分段 LRU 合同——晋升/降级/淘汰三分
 * 面、扫描不污染保护段、upsert 晋升、containsKey 透视、
 * 读数配平、fail-fast。
 */
class SlruCacheTest {

    private static final int CAPACITY = 4;

    private static final double HALF_PROTECTED = 0.5;

    private SlruCache<String, String> cache() {
        return new SlruCache<>(CAPACITY, HALF_PROTECTED);
    }

    @Test
    void overflowShouldEvictProbationaryHeadFirst() {
        SlruCache<String, String> slru = cache();
        for (String key : new String[]{"a", "b", "c", "d"}) {
            slru.put(key, key);
        }
        assertThat(slru.size()).isEqualTo(4);
        assertThat(slru.evictedCount()).isZero();
        slru.put("e", "e");
        assertThat(slru.containsKey("a")).isFalse();
        assertThat(slru.containsKey("e")).isTrue();
        assertThat(slru.evictedCount()).isEqualTo(1);
    }

    @Test
    void hitShouldPromoteAndProtectedOverflowShouldDemote() {
        SlruCache<String, String> slru = cache();
        for (String key : new String[]{"a", "b", "c", "d"}) {
            slru.put(key, key);
        }
        assertThat(slru.get("b")).hasValue("b");
        assertThat(slru.protectedSize()).isEqualTo(1);
        assertThat(slru.get("c")).hasValue("c");
        assertThat(slru.protectedSize()).isEqualTo(2);
        assertThat(slru.probationarySize()).isEqualTo(2);
        slru.get("a");
        assertThat(slru.protectedSize()).isEqualTo(2);
        assertThat(slru.probationarySize()).isEqualTo(2);
        assertThat(slru.probationarySize() + slru.protectedSize()).isEqualTo(4);
        assertThat(slru.containsKey("b")).isTrue();
        slru.put("e", "e");
        assertThat(slru.containsKey("d")).isFalse();
        assertThat(slru.containsKey("b")).isTrue();
        assertThat(slru.evictedCount()).isEqualTo(1);
    }

    @Test
    void fullScanShouldNotWipeProtectedHotKeys() {
        SlruCache<String, String> slru = cache();
        for (String key : new String[]{"a", "b", "c", "d"}) {
            slru.put(key, key);
        }
        slru.get("a");
        slru.get("b");
        for (String key : new String[]{"x", "y", "z", "w"}) {
            slru.put(key, key);
        }
        assertThat(slru.get("a")).hasValue("a");
        assertThat(slru.get("b")).hasValue("b");
        assertThat(slru.evictedCount()).isEqualTo(4);
    }

    @Test
    void upsertShouldOverwriteAndPromote() {
        SlruCache<String, String> slru = cache();
        slru.put("k", "v1");
        assertThat(slru.probationarySize()).isEqualTo(1);
        slru.put("k", "v2");
        assertThat(slru.probationarySize()).isZero();
        assertThat(slru.protectedSize()).isEqualTo(1);
        assertThat(slru.getOrDefault("k", "fallback")).isEqualTo("v2");
        assertThat(slru.size()).isEqualTo(1);
    }

    @Test
    void containsKeyShouldNotPromote() {
        SlruCache<String, String> slru = cache();
        slru.put("k", "v");
        assertThat(slru.containsKey("k")).isTrue();
        assertThat(slru.probationarySize()).isEqualTo(1);
        assertThat(slru.protectedSize()).isZero();
        assertThat(slru.get("missing")).isEmpty();
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new SlruCache<>(0, HALF_PROTECTED)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SlruCache<>(4, 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SlruCache<>(4, 1.0)).isInstanceOf(IllegalArgumentException.class);
        SlruCache<String, String> slru = cache();
        assertThatThrownBy(() -> slru.put(null, "v")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> slru.put("k", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> slru.get(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> slru.containsKey(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
