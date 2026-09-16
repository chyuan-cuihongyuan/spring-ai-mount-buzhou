package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2063 / T3228：层级令牌桶合同——双闸扣减、父顶硬顶（子合打不
 * 破）、子桶隔离、再充值封顶、快照对账、畸形 fail-fast。
 */
class HierarchicalTokenBucketTest {

    @Test
    void consumeShouldDeductBothLevels() {
        HierarchicalTokenBucket htb = new HierarchicalTokenBucket(100);
        htb.registerChild("tenant-a", 50);
        htb.registerChild("tenant-b", 50);
        assertThat(htb.tryConsume("tenant-a", 30)).isTrue();
        Map<String, Double> snap = htb.snapshot();
        assertThat(snap.get("(parent)")).isEqualTo(70.0d);   // 父扣 30
        assertThat(snap.get("tenant-a")).isEqualTo(20.0d);   // 子扣 30
        assertThat(snap.get("tenant-b")).isEqualTo(50.0d);   // 旁桶不动
    }

    @Test
    void parentCapShouldBeHardCeiling() {
        // 子合容量 200 > 父 100——合打不破父顶
        HierarchicalTokenBucket htb = new HierarchicalTokenBucket(100);
        htb.registerChild("a", 100);
        htb.registerChild("b", 100);
        assertThat(htb.tryConsume("a", 100)).isTrue();  // 父尽
        assertThat(htb.tryConsume("b", 1)).isFalse();   // 父空——b 有子币也借不到
        assertThat(htb.snapshot().get("b")).isEqualTo(100.0d); // b 未扣
    }

    @Test
    void childCapShouldSelfLimit() {
        HierarchicalTokenBucket htb = new HierarchicalTokenBucket(1000);
        htb.registerChild("small", 10);
        assertThat(htb.tryConsume("small", 10)).isTrue();
        assertThat(htb.tryConsume("small", 1)).isFalse();  // 子尽自限（父还富余）
        assertThat(htb.snapshot().get("(parent)")).isEqualTo(990.0d);
    }

    @Test
    void refillShouldCapAtCapacities() {
        HierarchicalTokenBucket htb = new HierarchicalTokenBucket(100);
        htb.registerChild("a", 50);
        htb.tryConsume("a", 50);
        htb.refill(999, Map.of("a", 999.0d)); // 超补
        Map<String, Double> snap = htb.snapshot();
        assertThat(snap.get("(parent)")).isEqualTo(100.0d); // 封顶容量
        assertThat(snap.get("a")).isEqualTo(50.0d);
    }

    @Test
    void partialRefillShouldAccumulate() {
        HierarchicalTokenBucket htb = new HierarchicalTokenBucket(100);
        htb.registerChild("a", 50);
        htb.tryConsume("a", 50);
        htb.refill(30, Map.of("a", 20.0d));
        assertThat(htb.snapshot().get("(parent)")).isEqualTo(80.0d); // 100−50 扣 +30 补
        assertThat(htb.snapshot().get("a")).isEqualTo(20.0d);
        assertThat(htb.tryConsume("a", 20)).isTrue();
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> new HierarchicalTokenBucket(0))
                .isInstanceOf(IllegalArgumentException.class);
        HierarchicalTokenBucket htb = new HierarchicalTokenBucket(10);
        assertThatThrownBy(() -> htb.registerChild(null, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> htb.registerChild(" ", 5))
                .isInstanceOf(IllegalArgumentException.class);
        htb.registerChild("a", 5);
        assertThatThrownBy(() -> htb.registerChild("a", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已注册");
        assertThatThrownBy(() -> htb.registerChild("b", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> htb.tryConsume("ghost", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> htb.tryConsume("a", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> htb.refill(-1, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> htb.refill(1, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> htb.refill(1, Map.of("ghost", 1.0)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
