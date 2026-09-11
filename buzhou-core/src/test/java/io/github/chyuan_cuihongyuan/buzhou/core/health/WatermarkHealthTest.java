package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.ContextWatermarkHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 526 / T801：水位告警桥接——低水位会话数达阈值 DOWN（真实 beforeModel
 * 驱动翻转态）、hook 未启用恒 UP、details 聚合读数、阈值校验 fail-fast。
 */
class WatermarkHealthTest {

    private static ContextWatermarkHook enabledHook() {
        // 窗口 1000 字符、低水位线 0.8——注入 >800 字符的轮次即进低水位区
        return new ContextWatermarkHook(new ContextWatermarkHook.Config(1000, 0.8));
    }

    private static ModelCallContext bigCtx(String sessionId) {
        ChatClientRequest request = new ChatClientRequest(
                new Prompt(java.util.List.of(new UserMessage("x".repeat(2000)))), Map.of());
        return new ModelCallContext() {
            @Override public String sessionId() { return sessionId; }
            @Override public String agentName() { return "a"; }
            @Override public int turn() { return 1; }
            @Override public SessionStateHandle state() {
                throw new UnsupportedOperationException();
            }
            @Override public void emitEvent(SessionEvent event) { }
            @Override public ChatClientRequest request() { return request; }
            @Override public ChatClientResponse response() { return null; }
            @Override public Throwable error() { return null; }
            @Override public void replaceRequest(ChatClientRequest newRequest) { }
            @Override public void replaceResponse(ChatClientResponse newResponse) { }
        };
    }

    @Test
    void lowWaterSessionsAtThresholdFlipHealthDown() {
        ContextWatermarkHook hook = enabledHook();
        WatermarkHealth health = new WatermarkHealth(hook, 2);

        // 两个会话进入低水位区（>800 字符 = 2000 字符请求）
        hook.beforeModel(bigCtx("sess-1"));
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP); // 1 < 2
        hook.beforeModel(bigCtx("sess-2"));
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.DOWN); // 2 ≥ 2 阈值
        assertThat(hook.lowWaterSessionCount()).isEqualTo(2);
        assertThat(health.details().get("lowWaterSessions")).isEqualTo(2);
        assertThat(health.details().get("lastUtilization")).isEqualTo(1.0);
    }

    @Test
    void disabledHookIsAlwaysUp() {
        ContextWatermarkHook disabled = new ContextWatermarkHook(
                ContextWatermarkHook.Config.disabled());
        WatermarkHealth health = new WatermarkHealth(disabled, 1);
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(health.mechanism()).isEqualTo("context-watermark");
        assertThat(health.details().get("enabled")).isEqualTo(false);
    }

    @Test
    void thresholdMustBePositive() {
        assertThatThrownBy(() -> new WatermarkHealth(enabledHook(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WatermarkHealth(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
