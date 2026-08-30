package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.error.QuotaExceededException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 124 §B / T449：虚拟 key 配额红队——限额内累计与快照；越限原子拒绝（不留
 * 部分扣减）+ 结构化异常；未注册 key 直通（诚实边界：不设预算 = 不拦）；注册
 * fail-fast（空白/非正/重复/表满 256）；窗口清零后恢复可用。借鉴：LiteLLM
 * virtual-key budgets。
 */
class VirtualKeysTest {

    @Test
    void spendUnderLimitAccumulatesAndReportsUsage() {
        VirtualKeys keys = VirtualKeys.create();
        keys.register("team-alpha", 1_000);

        assertThat(keys.trySpend("team-alpha", 400)).isTrue();
        assertThat(keys.trySpend("team-alpha", 350)).isTrue();
        assertThat(keys.usage("team-alpha").usedTokens()).isEqualTo(750);
        assertThat(keys.usage("team-alpha").limitTokens()).isEqualTo(1_000);
        assertThat(keys.topUsage(10)).extracting(VirtualKeys.KeyUsage::key)
                .containsExactly("team-alpha");
    }

    @Test
    void overLimitRejectedAtomicallyWithStructuredException() {
        VirtualKeys keys = VirtualKeys.create();
        keys.register("team-beta", 600);
        assertThat(keys.trySpend("team-beta", 600)).isTrue();

        // 越限：整体拒绝，已用不涨（无部分扣减）
        assertThat(keys.trySpend("team-beta", 1)).isFalse();
        assertThat(keys.usage("team-beta").usedTokens()).isEqualTo(600);

        assertThatThrownBy(() -> keys.spendOrThrow("team-beta", 5))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("team-beta")
                .hasMessageContaining("600")
                .hasMessageContaining("limit 600");
    }

    @Test
    void unregisteredKeyPassesThroughAndUsageIsNull() {
        VirtualKeys keys = VirtualKeys.create();
        assertThat(keys.trySpend("ghost", Long.MAX_VALUE / 2)).isTrue();
        assertThat(keys.usage("ghost")).isNull();
        keys.reset("ghost"); // 未注册 key no-op 诚实
        assertThat(keys.distinct()).isZero();
    }

    @Test
    void registerValidatesAndBounded() {
        VirtualKeys keys = VirtualKeys.create();
        assertThatThrownBy(() -> keys.register(" ", 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> keys.register("k", 0))
                .isInstanceOf(IllegalArgumentException.class);
        keys.register("dup", 100);
        assertThatThrownBy(() -> keys.register("dup", 200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");

        VirtualKeys full = VirtualKeys.create();
        for (int i = 0; i < VirtualKeys.MAX_KEYS; i++) {
            full.register("key-" + i, 100);
        }
        assertThatThrownBy(() -> full.register("one-too-many", 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registry full");
        assertThat(full.distinct()).isEqualTo(VirtualKeys.MAX_KEYS);
    }

    @Test
    void resetClearsWindowAndSpendResumes() {
        VirtualKeys keys = VirtualKeys.create();
        keys.register("ops", 100);
        keys.spendOrThrow("ops", 100);
        assertThat(keys.trySpend("ops", 1)).isFalse();

        keys.reset("ops"); // export → reset 循环：每窗口一份账
        assertThat(keys.usage("ops").usedTokens()).isZero();
        assertThat(keys.trySpend("ops", 100)).isTrue();
    }

    @Test
    void negativeTokensRejectedAndZeroIsNoop() {
        VirtualKeys keys = VirtualKeys.create();
        keys.register("k", 10);
        assertThatThrownBy(() -> keys.trySpend("k", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(keys.trySpend("k", 0)).isTrue();
        assertThat(keys.usage("k").usedTokens()).isZero();
        List<VirtualKeys.KeyUsage> top = keys.topUsage(1);
        assertThat(top.get(0).key()).isEqualTo("k");
    }
}
