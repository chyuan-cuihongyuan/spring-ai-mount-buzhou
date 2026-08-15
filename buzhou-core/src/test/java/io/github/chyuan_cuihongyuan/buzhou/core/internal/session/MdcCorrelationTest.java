package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 47 §A / T172 / impl-141：MDC 会话轮次关联端到端。
 * 覆盖面 = 轮次调用线程（会话层日志主产地）；跨线程传播是显式排除的诚实边界。
 */
class MdcCorrelationTest {

    @AfterEach
    void cleanMdc() {
        MDC.remove(DefaultAgentSession.MDC_SESSION_ID);
        MDC.remove(DefaultAgentSession.MDC_TURN_SEQ);
    }

    /** chat 轮次期间观察者回调（同线程）可见两键；轮次结束必清。 */
    @Test
    void chatTurnCarriesMdcAndClearsAfter() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("回复"));
        StringBuilder seenSession = new StringBuilder();
        StringBuilder seenTurn = new StringBuilder();
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.assemblyCustomizers(
                        java.util.List.of(ctx -> ctx.addObserver(new SessionObserver() {
                            @Override
                            public void onTurnStart(int turnSeq, String input) {
                                seenSession.append(MDC.get(DefaultAgentSession.MDC_SESSION_ID));
                                seenTurn.append(MDC.get(DefaultAgentSession.MDC_TURN_SEQ));
                            }
                        }))));
        AgentSession session = runtime.spawn("app", "agent", "sess-mdc");

        String reply = session.chat("问一句");

        assertThat(reply).isEqualTo("回复");
        assertThat(seenSession.toString()).isEqualTo("sess-mdc");
        assertThat(seenTurn.toString()).isEqualTo("1");
        assertThat(MDC.get(DefaultAgentSession.MDC_SESSION_ID)).isNull();
        assertThat(MDC.get(DefaultAgentSession.MDC_TURN_SEQ)).isNull();
        session.close();
    }

    /** 模型抛错的轮次：MDC 仍被清除（finally 语义）。 */
    @Test
    void failedTurnStillClearsMdc() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.failNext = true;
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores());
        AgentSession session = runtime.spawn("app", "agent", "sess-mdc-err");

        try {
            session.chat("会炸的问句");
        } catch (RuntimeException expected) {
            // 模型错误上抛（所期）
        }

        assertThat(MDC.get(DefaultAgentSession.MDC_SESSION_ID)).isNull();
        assertThat(MDC.get(DefaultAgentSession.MDC_TURN_SEQ)).isNull();
        session.close();
    }

    /**
     * 流式路径不写 MDC（实现期裁定钉住）：Spring AI 流式管线把信号发射切到 boundedElastic
     * 线程——put 落订阅线程、remove 落发射线程 = 清错线程泄漏。结构性限制，chat 路径专属。
     */
    @Test
    void streamPathDeliberatelyDoesNotTouchMdc() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("流式回复"));
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores());
        AgentSession session = runtime.spawn("app", "agent", "sess-mdc-stream");

        session.stream("流式问一句")
                .doOnNext(r -> assertThat(MDC.get(DefaultAgentSession.MDC_SESSION_ID)).isNull())
                .blockLast();

        assertThat(MDC.get(DefaultAgentSession.MDC_SESSION_ID)).isNull();
        assertThat(MDC.get(DefaultAgentSession.MDC_TURN_SEQ)).isNull();
        session.close();
    }

    static class ScriptedChatModel implements ChatModel {
        private final Queue<ChatResponse> script = new ConcurrentLinkedQueue<>();
        boolean failNext;

        void enqueue(AssistantMessage message) {
            script.add(new ChatResponse(List.of(new Generation(message))));
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("模型故障（测试脚本）");
            }
            ChatResponse next = script.poll();
            return next != null ? next
                    : new ChatResponse(List.of(new Generation(new AssistantMessage("default"))));
        }

        @Override
        public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
            return reactor.core.publisher.Flux.just(call(prompt));
        }
    }
}
