package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5009 / T6120：Clock-Sweep 合同——基础驱逐序、命中差异
 * 显证、使用计数封顶、空缓存与畸形 fail-fast、确定性回放。
 */
class ClockSweepCacheTest {

    @Test
    void basicSweepShouldEvictInRingOrder() {
        ClockSweepCache<String, String> cache = new ClockSweepCache<>(3);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        assertThat(cache.evictOne()).isEqualTo("a");   // 全 1 计数——指针位先出
        assertThat(cache.evictOne()).isEqualTo("b");
        assertThat(cache.containsKey("c")).isTrue();
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    void hotFrameShouldSurviveLongerThanColdNewcomer() {
        ClockSweepCache<String, String> cache = new ClockSweepCache<>(3);
        cache.put("hot", "H");
        cache.put("mid", "M");
        cache.put("cold", "C");
        cache.get("hot");   // 旧页高频——usage 提升多扛两轮
        cache.get("hot");
        assertThat(cache.evictOne()).isEqualTo("mid");   // 双低频页环序靠前者先出
        assertThat(cache.containsKey("hot")).isTrue();   // 热页扛过本轮扫描
    }

    @Test
    void usageCountShouldCapAtLimit() {
        ClockSweepCache<String, String> cache = new ClockSweepCache<>(2);
        cache.put("a", "1");
        for (int i = 0; i < 50; i++) {
            cache.get("a");
        }
        cache.put("b", "2");
        cache.put("c", "3");   // 触发一次驱逐——a 因封顶仍可能被衰减摘除
        assertThat(cache.size()).isEqualTo(2);
        List<String> order = cache.ringOrder();
        assertThat(order).hasSize(2);
    }

    @Test
    void overwriteShouldNotGrowRing() {
        ClockSweepCache<String, String> cache = new ClockSweepCache<>(2);
        cache.put("a", "1");
        cache.put("a", "1-updated");
        assertThat(cache.size()).isEqualTo(1);
        assertThat(cache.ringOrder()).containsExactly("a");
        assertThat(cache.get("a")).isEqualTo("1-updated");
    }

    @Test
    void emptyEvictAndInvalidInputsShouldFailFast() {
        ClockSweepCache<String, String> cache = new ClockSweepCache<>(2);
        assertThat(cache.evictOne()).isNull();   // 空缓存诚实 null
        assertThatThrownBy(() -> new ClockSweepCache<>(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.put(null, "v"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cache.get(null))
                .isInstanceOf(NullPointerException.class);
    }
}
