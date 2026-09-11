package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.StreamTextFilter;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 500 / T751：回复流出站过滤 SPI 缝——流式（逐 chunk 过滤 + 收口 flush
 * chunk 进订阅者）与非流式（整段 filter+flush）两缝端到端；无 filter 钩子时
 * chunk 原样透传（零行为变化回归）。guard 的 PiiStreamRedactionHook 为真实
 * 消费方（guard 模块自测）；此处用测试内联过滤器钉住 core 缝语义。
 */
class ReplyStreamFilterSeamTest {

    /** 连续数字段 ≥5 位掩码（窗口 8——跨界数字段留窗待完整）。 */
    static final class DigitMaskFilter implements StreamTextFilter {
        private static final Pattern RUN = Pattern.compile("\\d{5,}");
        private static final int HOLD_BACK = 8;

        private final StringBuilder pending = new StringBuilder();

        @Override
        public String filter(String chunk) {
            pending.append(chunk);
            return drain(false);
        }

        @Override
        public String flush() {
            return drain(true);
        }

        private String drain(boolean finalPass) {
            String masked = RUN.matcher(pending).replaceAll("[MASKED]");
            pending.replace(0, pending.length(), masked);
            int emitLength = finalPass
                    ? pending.length()
                    : Math.max(0, pending.length() - HOLD_BACK + 1);
            if (emitLength == 0) {
                return "";
            }
            String outbound = pending.substring(0, emitLength);
            pending.delete(0, emitLength);
            return outbound;
        }
    }

    static BuzhouHook filterHook() {
        return new BuzhouHook() {
            @Override
            public StreamTextFilter replyStreamFilter() {
                return new DigitMaskFilter();
            }
        };
    }

    /** 多 chunk 脚本化流式模型。 */
    static final class ScriptedStreamChatModel implements ChatModel {
        private final List<String> chunks;

        ScriptedStreamChatModel(List<String> chunks) {
            this.chunks = chunks;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(
                    new AssistantMessage(String.join("", chunks)))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.fromIterable(chunks).map(text ->
                    new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }
    }

    private static AgentRuntime runtime(ChatModel model, List<BuzhouHook> hooks) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        return Buzhou.runtime(model, stores,
                new RuntimeConfig(hooks, java.util.Set.of(), java.util.Set.of(), null, List.of()));
    }

    @Test
    void streamSubscriberSeesOnlyMaskedChunksIncludingFlushTail() {
        // 数字段跨三 chunk 断裂（任一中间片都不足 5 位）+ 回复短于窗口：全部滞窗，
        // 前缀只在窗口滑过后放行、掩码段由收口 flush 补齐到达
        ScriptedStreamChatModel model = new ScriptedStreamChatModel(
                List.of("客服电话 ", "138", "00138000 "));
        AgentRuntime runtime = runtime(model, List.of(filterHook()));
        AgentSession session = runtime.spawn("app", "support", "sess-mask");

        List<String> seen = new CopyOnWriteArrayList<>();
        session.stream("热线多少").doOnNext(resp -> {
                    String text = resp.getResult() == null ? null
                            : resp.getResult().getOutput().getText();
                    if (text != null) {
                        seen.add(text);
                    }
                })
                .blockLast();

        String joined = String.join("", seen);
        assertThat(joined).isEqualTo("客服电话 [MASKED] ");
        assertThat(joined).doesNotContain("13800138000");
        // 收口 flush 作为末尾元素到达（掩码段尾部滞窗到轮终才放行——非 flush 则无末片）
        assertThat(seen.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void nonStreamChatReplyIsMaskedViaFilterPlusFlush() {
        ScriptedStreamChatModel model = new ScriptedStreamChatModel(
                List.of("工单号 20260911 处理中"));
        AgentRuntime runtime = runtime(model, List.of(filterHook()));
        AgentSession session = runtime.spawn("app", "support", "sess-chat");

        String reply = session.chat("工单到哪了");
        assertThat(reply).isEqualTo("工单号 [MASKED] 处理中");
    }

    @Test
    void withoutFilterHookStreamIsUntouched() {
        List<String> chunks = List.of("abc", "12345", "def");
        ScriptedStreamChatModel model = new ScriptedStreamChatModel(chunks);
        AgentRuntime runtime = runtime(model, List.of());
        AgentSession session = runtime.spawn("app", "support", "sess-raw");

        List<String> seen = new CopyOnWriteArrayList<>();
        session.stream("raw").doOnNext(resp -> {
                    String text = resp.getResult() == null ? null
                            : resp.getResult().getOutput().getText();
                    if (text != null) {
                        seen.add(text);
                    }
                })
                .blockLast();

        assertThat(seen).containsExactlyElementsOf(chunks);
    }
}
