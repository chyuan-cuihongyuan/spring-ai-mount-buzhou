package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 停机排水取消原因 E2E（spec 606 扩散 / T914–T915 / impl 485）：runtime 优雅停机对
 * 在途会话发 AFTER_CURRENT_TURN 取消——session.cancelled 事件 cause=SHUTDOWN_DRAIN
 * （loop7 接线的真路径验证，此前仅签名兼容覆盖）。
 */
class ShutdownDrainCauseEndToEndTest {

    /** 挂死模型（chat 永不返回——在途 Turn 语义）。 */
    private static final class HangingChatModel extends ScriptedChatModel {
        final CountDownLatch never = new CountDownLatch(1);

        @Override
        public org.springframework.ai.chat.model.ChatResponse call(
                org.springframework.ai.chat.prompt.Prompt prompt) {
            try {
                never.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return super.call(prompt);
        }
    }

    /** 停机排水：在途会话收到 session.cancelled {AFTER_CURRENT_TURN, SHUTDOWN_DRAIN}。 */
    @Test
    void shutdownDrainEmitsCancelCause() throws Exception {
        HangingChatModel model = new HangingChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        BuzhouHook capture = new BuzhouHook() {
            @Override
            public void onEvent(io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionEventContext ctx) {
                events.add(ctx.event());
            }
        };
        var runtime = Buzhou.runtime(model, stores,
                RuntimeConfig.merge(RuntimeConfig.defaults(), RuntimeConfig.hooks(List.of(capture))));
        var session = runtime.spawn("app", "agent", "s-drain");

        CompletableFuture<String> chat = CompletableFuture.supplyAsync(() -> session.chat("q"));
        Thread.sleep(200); // chat 进入模型阻塞（在途）

        io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime concrete =
                (io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime) runtime;
        CompletableFuture<Boolean> shutdown = CompletableFuture.supplyAsync(
                () -> concrete.shutdownGracefully(Duration.ofSeconds(2)));
        Thread.sleep(500); // 停机②步：对在途会话发 AFTER_CURRENT_TURN

        SessionEvent cancelled = events.stream()
                .filter(e -> "session.cancelled".equals(e.type())).findFirst().orElse(null);
        assertThat(cancelled).as("停机排水应发 session.cancelled").isNotNull();
        assertThat(cancelled.payload()).containsEntry("cancelMode", "AFTER_CURRENT_TURN");
        assertThat(cancelled.payload()).containsEntry("cause", "SHUTDOWN_DRAIN");

        shutdown.get(5, java.util.concurrent.TimeUnit.SECONDS);
        chat.cancel(true); // 清理挂死 future
    }
}
