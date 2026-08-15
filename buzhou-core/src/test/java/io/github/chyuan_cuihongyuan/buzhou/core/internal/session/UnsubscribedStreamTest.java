package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 50 §B / T180 / impl-149：未订阅流计数残留修复——stream 轮次占用惰性化（Flux.defer）。
 */
class UnsubscribedStreamTest {

    /** 未订阅：不占单飞闸——后续 chat 照常（既往残留 +1 会卡死闸至 close）。 */
    @Test
    void unsubscribedStreamDoesNotOccupyTurnSlot() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("chat 回复"));
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores());
        AgentSession session = runtime.spawn("app", "agent", "sess-unsub");

        // 返回但从不订阅（不消费脚本——chat 拿到首条）
        session.stream("这条流没人订阅");
        // 闸未被占：chat 正常走（既往此处 TURN_IN_FLIGHT）
        assertThat(session.chat("换 chat 通道")).isEqualTo("chat 回复");
        session.close();
    }

    /** 同一 Flux 顺序复订阅：终结后再订阅 = 重新开轮（defer 体重放语义钉住）。 */
    @Test
    void sequentialResubscriptionRunsANewTurn() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("第一次"));
        model.enqueue(new AssistantMessage("第二次"));
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores());
        AgentSession session = runtime.spawn("app", "agent", "sess-resub");

        reactor.core.publisher.Flux<ChatResponse> flux = session.stream("问");
        assertThat(flux.collectList().block().getFirst().getResult().getOutput().getText())
                .isEqualTo("第一次");

        // 第一次已终结（闸释放）→ 第二次订阅会重新开轮（defer 体重放）——本例脚本仍有第二条回复
        assertThat(flux.collectList().block().getFirst().getResult().getOutput().getText())
                .isEqualTo("第二次");
        session.close();
    }

    /** 订阅中的流仍占闸：在途流未终结时第二个轮次入口确定拒绝（单飞闸语义回归）。 */
    @Test
    void inFlightStreamStillBlocksSecondTurn() {
        SlowEmitModel model = new SlowEmitModel();
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores());
        AgentSession session = runtime.spawn("app", "agent", "sess-inflight");

        var disposable = session.stream("慢流").subscribe();
        model.awaitStarted();
        assertThatThrownBy(() -> session.chat("挤进来"))
                .isInstanceOf(BuzhouException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TURN_IN_FLIGHT);
        model.release();
        disposable.dispose();
        session.close();
    }

    static class ScriptedChatModel implements ChatModel {
        private final Queue<ChatResponse> script = new ConcurrentLinkedQueue<>();

        void enqueue(AssistantMessage message) {
            script.add(new ChatResponse(List.of(new Generation(message))));
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            ChatResponse next = script.poll();
            return next != null ? next
                    : new ChatResponse(List.of(new Generation(new AssistantMessage("default"))));
        }

        @Override
        public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
            return reactor.core.publisher.Flux.just(call(prompt));
        }
    }

    /** 首块挂住直至 release 的慢流模型（单飞闸在途窗口用）。 */
    static class SlowEmitModel implements ChatModel {
        private final java.util.concurrent.CountDownLatch started = new java.util.concurrent.CountDownLatch(1);
        private final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);

        void awaitStarted() {
            try {
                started.await(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        void release() {
            release.countDown();
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage("fallback"))));
        }

        @Override
        public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
            return reactor.core.publisher.Flux.defer(() -> {
                started.countDown();
                try {
                    release.await(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return reactor.core.publisher.Flux.just(call(prompt));
            });
        }
    }
}
