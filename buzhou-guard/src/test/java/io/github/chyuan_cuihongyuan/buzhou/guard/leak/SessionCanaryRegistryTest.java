package io.github.chyuan_cuihongyuan.buzhou.guard.leak;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 528 / T809–T810：跨会话泄漏金丝雀——确定性令牌、跨会话命中判定
 * （观察者自身令牌不算）、无令牌文本零误报、LRU 有界、salt 影响、校验
 * fail-fast（thinkst canarytokens/honeytoken 思想）。
 */
class SessionCanaryRegistryTest {

    @Test
    void crossSessionTokenHitIsDetectedWithOwner() {
        SessionCanaryRegistry registry = new SessionCanaryRegistry("s3cret");
        String canaryA = registry.plant("tenant-a");
        String canaryB = registry.plant("tenant-b");

        // B 的回复里出现 A 的金丝雀 = 跨会话泄漏
        var leaks = registry.detect("tenant-b", "回答内容 " + canaryA + " 完毕");
        assertThat(leaks).hasSize(1);
        assertThat(leaks.getFirst().leakedFromSession()).isEqualTo("tenant-a");
        // 观察者自己的令牌不算泄漏（正常回显）
        assertThat(registry.detect("tenant-b", "回显 " + canaryB)).isEmpty();
    }

    @Test
    void deterministicTokensAndSaltSeparated() {
        SessionCanaryRegistry r1 = new SessionCanaryRegistry("s3cret");
        SessionCanaryRegistry r2 = new SessionCanaryRegistry("pepper");
        assertThat(r1.plant("a")).isEqualTo(r1.plant("a")); // 确定性
        assertThat(r1.plant("a")).isNotEqualTo(r2.plant("a")); // salt 隔离
        assertThatThrownBy(() -> new SessionCanaryRegistry(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cleanTextAndUnplantedTokensProduceNothing() {
        SessionCanaryRegistry registry = new SessionCanaryRegistry("s3cret");
        registry.plant("a");
        assertThat(registry.detect("b", "完全正常的回复")).isEmpty();
        // 未种植会话的伪造令牌形态——不误报（注册表只认已种令牌）
        assertThat(registry.detect("b", "BUZHOU-LEAKCANARY-deadbeef 以及 BUZHOU-LEAKCANARY-ab")).isEmpty();
    }

    @Test
    void registryIsBoundedLru() {
        SessionCanaryRegistry registry = new SessionCanaryRegistry("s3cret", 4);
        for (int i = 0; i < 10; i++) {
            registry.plant("s" + i);
        }
        assertThat(registry.size()).isEqualTo(4); // LRU 有界
    }
}
