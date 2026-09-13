package io.github.chyuan_cuihongyuan.buzhou.guard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 820 / T1142：豁免登记面回归——登记/判定/过期惰性失效/续期覆盖/
 * 撤销/封顶/快照仅未过期/脏参数忽略。
 */
class GuardExemptionRegistryTest {

    @Test
    void grantExemptRevokeRoundtrip() {
        GuardExemptionRegistry registry = new GuardExemptionRegistry();

        assertThat(registry.exempt("secret-scan", "tool_x", 100)).isFalse();
        assertThat(registry.grant("secret-scan", "tool_x", 1000, "已审计误报")).isTrue();
        assertThat(registry.exempt("secret-scan", "tool_x", 500)).isTrue();
        assertThat(registry.exempt("secret-scan", "other", 500)).isFalse(); // 主体隔离
        assertThat(registry.exempt("pii-redact", "tool_x", 500)).isFalse(); // 机制隔离

        assertThat(registry.revoke("secret-scan", "tool_x")).isTrue();
        assertThat(registry.exempt("secret-scan", "tool_x", 500)).isFalse();
        assertThat(registry.revoke("secret-scan", "tool_x")).isFalse(); // 幂等
    }

    @Test
    void expiryIsLazyAndCounted() {
        GuardExemptionRegistry registry = new GuardExemptionRegistry();
        registry.grant("m", "s", 1000, "r");

        assertThat(registry.exempt("m", "s", 999)).isTrue();   // now < until
        assertThat(registry.exempt("m", "s", 1000)).isFalse(); // now == until → 过期

        // 过期条目已被惰性移除——再次判定也是 false 且 expiredTotal 只计一次
        assertThat(registry.exempt("m", "s", 1001)).isFalse();
        assertThat(registry.snapshot(2000).expiredTotal()).isEqualTo(1);
        assertThat(registry.snapshot(2000).active()).isEmpty();
    }

    @Test
    void grantOverwritesAsRenewal() {
        GuardExemptionRegistry registry = new GuardExemptionRegistry();
        registry.grant("m", "s", 1000, "首次");
        registry.grant("m", "s", 5000, "续期");
        assertThat(registry.exempt("m", "s", 4000)).isTrue(); // 续期生效
        assertThat(registry.snapshot(0).grantedTotal()).isEqualTo(2);
        assertThat(registry.snapshot(0).active()).hasSize(1);
        assertThat(registry.snapshot(0).active().get(0).reason()).isEqualTo("续期");
    }

    @Test
    void entriesAreCapped() {
        GuardExemptionRegistry registry = new GuardExemptionRegistry();
        for (int i = 0; i < GuardExemptionRegistry.MAX_ENTRIES; i++) {
            assertThat(registry.grant("m", "s" + i, 10_000, "r")).isTrue();
        }
        assertThat(registry.grant("m", "overflow", 10_000, "r")).isFalse();
        assertThat(registry.snapshot(0).truncated()).isTrue();
        assertThat(registry.snapshot(0).active()).hasSize(GuardExemptionRegistry.MAX_ENTRIES);
    }

    @Test
    void dirtyArgsIgnoredAndSnapshotSorted() {
        GuardExemptionRegistry registry = new GuardExemptionRegistry();
        assertThat(registry.grant(null, "s", 100, "r")).isFalse();
        assertThat(registry.grant("m", " ", 100, "r")).isFalse();
        assertThat(registry.grant("m", "s", 0, "r")).isFalse();
        assertThat(registry.grant("m", "s", -1, null)).isFalse();
        assertThat(registry.revoke(null, "s")).isFalse();

        registry.grant("a", "x", 500, "r1");
        registry.grant("b", "y", 900, "r2");
        registry.grant("c", "z", 700, "r3");
        var snap = registry.snapshot(0);
        // until 降序：900 → 700 → 500
        assertThat(snap.active().get(0).mechanism()).isEqualTo("b");
        assertThat(snap.active().get(1).mechanism()).isEqualTo("c");
        assertThat(snap.active().get(2).mechanism()).isEqualTo("a");
    }
}
