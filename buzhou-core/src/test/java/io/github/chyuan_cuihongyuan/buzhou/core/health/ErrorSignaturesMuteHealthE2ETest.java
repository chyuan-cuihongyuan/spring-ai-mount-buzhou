package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.ErrorSignatures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 静默标记×健康段联动补验（spec 742 / T1033–T1034 / impl 544）：mute 签名
 * 从健康段 details 的 top 列表消失（top 排除自动传导）、snapshot 原样、unmute 回归。
 */
class ErrorSignaturesMuteHealthE2ETest {

    @SuppressWarnings("unchecked")
    private static java.util.List<String> topRows(ErrorSignaturesHealth health) {
        return (java.util.List<String>) health.details().get("top");
    }

    @Test
    void mutePropagatesToHealthDetails() {
        ErrorSignatures registry = ErrorSignatures.create();
        registry.record("tool", "TimeoutException:slow");
        registry.record("tool", "TimeoutException:slow");
        registry.record("tool", "NullPointerException:boom");
        ErrorSignaturesHealth health = new ErrorSignaturesHealth(registry);

        // mute 前：慢超时在 details 的 top 列表
        assertThat(topRows(health)).anySatisfy(row -> assertThat(row).startsWith("tool:TimeoutException:slow"));

        // mute 后：top 列表消失（top 排除自动传导）
        registry.mute("tool:TimeoutException:slow");
        assertThat(topRows(health)).noneSatisfy(row -> assertThat(row).startsWith("tool:TimeoutException:slow"));
        assertThat(topRows(health)).anySatisfy(row -> assertThat(row).startsWith("tool:NullPointerException:boom"));

        // 原始事实可查：snapshot 原样含 muted
        assertThat(registry.snapshot()).containsEntry("tool:TimeoutException:slow", 2L);

        // unmute：健康段回归
        registry.unmute("tool:TimeoutException:slow");
        assertThat(topRows(health)).anySatisfy(row -> assertThat(row).startsWith("tool:TimeoutException:slow"));
    }
}
