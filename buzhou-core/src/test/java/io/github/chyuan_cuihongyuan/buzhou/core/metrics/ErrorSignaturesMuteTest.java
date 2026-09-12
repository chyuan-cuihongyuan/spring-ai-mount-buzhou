package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 错误签名静默标记测试（spec 720 / T991–T992 / impl 523）：mute 后 top 消失
 * 但 snapshot 照常、unmute 回归、cap 拒绝、reset 清空、kind 分面同样排除。
 */
class ErrorSignaturesMuteTest {

    @Test
    void mutedSignatureExcludedFromTopButCounted() {
        ErrorSignatures registry = ErrorSignatures.create();
        registry.record("tool", "TimeoutException:slow");
        registry.record("tool", "TimeoutException:slow");
        registry.record("tool", "NullPointerException:boom");
        String mutedSig = ErrorSignatures.signature("tool", "TimeoutException:slow");

        assertThat(registry.mute(mutedSig)).isTrue();

        List<Map.Entry<String, Long>> top = registry.top(10);
        assertThat(top).extracting(Map.Entry::getKey).doesNotContain(mutedSig);
        assertThat(top).extracting(Map.Entry::getKey).contains("tool:NullPointerException:boom");

        // 计数照常累计——原始事实可查
        assertThat(registry.snapshot()).containsEntry(mutedSig, 2L);
        // 记录继续累计
        registry.record("tool", "TimeoutException:slow");
        assertThat(registry.snapshot()).containsEntry(mutedSig, 3L);
    }

    @Test
    void unmuteRestoresTopVisibility() {
        ErrorSignatures registry = ErrorSignatures.create();
        registry.record("model", "RateLimit:429");
        String sig = ErrorSignatures.signature("model", "RateLimit:429");
        registry.mute(sig);
        assertThat(registry.top(10)).isEmpty();

        assertThat(registry.unmute(sig)).isTrue();
        assertThat(registry.top(10)).extracting(Map.Entry::getKey).contains(sig);
    }

    @Test
    void kindFacetAlsoExcludesMuted() {
        ErrorSignatures registry = ErrorSignatures.create();
        registry.record("tool", "Boom:1");
        registry.record("tool", "Okay:1");
        registry.mute(ErrorSignatures.signature("tool", "Boom:1"));

        List<Map.Entry<String, Long>> facet = registry.top("tool", 10);
        // 签名归一化：数字折 #（signature 规范）
        assertThat(facet).extracting(Map.Entry::getKey).contains("tool:Okay:#");
        assertThat(facet).extracting(Map.Entry::getKey).doesNotContain("tool:Boom:#");
    }

    @Test
    void muteCapRejectsOverflow() {
        ErrorSignatures registry = ErrorSignatures.create();
        for (int i = 0; i < ErrorSignatures.MUTED_CAP; i++) {
            assertThat(registry.mute("sig-" + i)).isTrue();
        }
        assertThat(registry.mute("sig-overflow")).isFalse(); // 第 65 个拒绝
        assertThat(registry.mute("sig-0")).isTrue(); // 已在册幂等
    }

    @Test
    void resetClearsMutedSet() {
        ErrorSignatures registry = ErrorSignatures.create();
        registry.record("tool", "Boom:1");
        registry.mute(ErrorSignatures.signature("tool", "Boom:1"));
        registry.reset();

        assertThat(registry.mutedSignatures()).isEmpty();
        registry.record("tool", "Boom:1");
        assertThat(registry.top(10)).isNotEmpty(); // 回归可见
    }

    @Test
    void mutedViewImmutableAndBlankRejected() {
        ErrorSignatures registry = ErrorSignatures.create();
        registry.mute("tool:Boom:1");
        assertThat(registry.mutedSignatures()).isUnmodifiable();
        assertThat(registry.mute("")).isFalse();
        assertThat(registry.mute(null)).isFalse();
    }
}
