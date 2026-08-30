package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.budget.VirtualKeys;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 154 §B / T507：虚拟 key 健康面红队——恒 UP 观测不裁决（耗尽由预算闸
 * 拦截）；top-8 有界（>8 key 只显前 8）；exhaustedKeys 全量计数与行内
 * exhausted 标记一致；null = UNKNOWN disabled。
 */
class VirtualKeysHealthTest {

    @Test
    @SuppressWarnings("unchecked")
    void reportsBoundedTopUsageWithExhaustedFlags() {
        VirtualKeys keys = VirtualKeys.create();
        keys.register("k1", 100);
        keys.register("k2", 50);
        keys.register("k3", 30);
        keys.trySpend("k1", 100); // 用满 → exhausted（used >= limit）
        keys.trySpend("k2", 10);

        VirtualKeysHealth health = new VirtualKeysHealth(keys);
        assertThat(health.mechanism()).isEqualTo("virtual-keys");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP); // 观测面恒 UP

        Map<String, Object> details = health.details();
        assertThat(details.get("distinctKeys")).isEqualTo(3);
        assertThat(details.get("exhaustedKeys")).isEqualTo(1L);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) details.get("topUsage");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).get("key")).isEqualTo("k1"); // used 降序
        assertThat(rows.get(0).get("exhausted")).isEqualTo(true);
        assertThat(rows.get(1).get("exhausted")).isEqualTo(false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void topRowsCappedAtEight() {
        VirtualKeys keys = VirtualKeys.create();
        for (int i = 0; i < 12; i++) {
            keys.register("k-" + i, 100);
            keys.trySpend("k-" + i, 100 - i); // used 递减——排行确定
        }
        Map<String, Object> details = new VirtualKeysHealth(keys).details();
        List<Map<String, Object>> rows = (List<Map<String, Object>>) details.get("topUsage");
        assertThat(rows).hasSize(8); // 有界封顶
        assertThat(details.get("distinctKeys")).isEqualTo(12); // 全量计数不截断
    }

    @Test
    void nullKeysMeansUnknownDisabled() {
        VirtualKeysHealth health = new VirtualKeysHealth(null);
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UNKNOWN);
        assertThat(health.details()).containsEntry("disabled", true);
    }
}
