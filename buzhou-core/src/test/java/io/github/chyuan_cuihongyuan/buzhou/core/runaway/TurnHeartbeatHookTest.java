package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 152 §B / T505：心跳钩子 e2e——轮次起止自动注册/清除（chat 进行中
 * inFlight=1 且 lastBeat 推进；结束后归零）；永续 CONTINUE 不干预裁决；
 * 串联在 RuntimeConfig 即生效（宿主零手工调用）。
 */
class TurnHeartbeatHookTest {

    @Test
    void turnLifecycleRegistersAndClearsWithBeatsInBetween() throws Exception {
        CountDownLatch modelEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ScriptedChatModel model = new ScriptedChatModel() {
            @Override
            public org.springframework.ai.chat.model.ChatResponse call(
                    org.springframework.ai.chat.prompt.Prompt prompt) {
                modelEntered.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS); // 悬挂模型调用：轮次在飞
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.call(prompt);
            }
        };
        model.enqueueText("r1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        TurnHeartbeatHook hook = new TurnHeartbeatHook(new TurnHeartbeat());
        var runtime = Buzhou.runtime(model, stores,
                new io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig(
                        List.of(hook), Set.of(), Set.of(), null, List.of()));

        assertThat(hook.heartbeat().inFlight()).isZero(); // 轮前：空表

        var session = runtime.spawn("app", "agent", "sess-hb");
        AtomicReference<Instant> beatDuringFlight = new AtomicReference<>();
        Thread chat = new Thread(() -> session.chat("慢问题"));
        chat.start();
        assertThat(modelEntered.await(3, TimeUnit.SECONDS)).isTrue();

        // 轮次在飞：已注册且有打点（beforeModel 至少一次）
        assertThat(hook.heartbeat().inFlight()).isEqualTo(1);
        assertThat(hook.heartbeat().lastBeat("sess-hb")).isNotNull();
        beatDuringFlight.set(hook.heartbeat().lastBeat("sess-hb"));

        // 在飞但持续打点：不判停滞（quiet < 阈值）
        assertThat(hook.heartbeat().stalled(List.of("sess-hb"),
                Duration.ofSeconds(30), Instant.now())).isEmpty();

        release.countDown();
        chat.join(5000);
        assertThat(hook.heartbeat().inFlight()).isZero(); // 轮后：清除
        assertThat(hook.heartbeat().lastBeat("sess-hb")).isNull();
        session.close();
    }

    @Test
    void nullHeartbeatGetsOwnInstanceNotNpe() {
        TurnHeartbeatHook hook = new TurnHeartbeatHook(null);
        assertThat(hook.name()).isEqualTo("TurnHeartbeatHook");
        assertThat(hook.order()).isEqualTo(TurnHeartbeatHook.ORDER);
        assertThat(hook.heartbeat()).isNotNull();
    }
}
