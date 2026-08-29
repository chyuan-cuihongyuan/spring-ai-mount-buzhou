package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 137 / T486：模型对冲回归——慢主快备先回先得（总耗时≈delay+快备）/
 * 主快返回零对冲 / 主快速失败转对冲 / 双败抛主异常 / stream 委派主不打扰备。
 */
class HedgedChatModelTest {

    /** 可控延迟/失败桩：delayMillis 后返回文本；throwInstead=true 直接抛错。 */
    private static final class StubModel implements ChatModel {
        private final String text;
        private final long delayMillis;
        private final boolean throwInstead;
        final List<Prompt> seenPrompts = new CopyOnWriteArrayList<>();
        final AtomicInteger calls = new AtomicInteger();

        StubModel(String text, long delayMillis, boolean throwInstead) {
            this.text = text;
            this.delayMillis = delayMillis;
            this.throwInstead = throwInstead;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            seenPrompts.add(prompt);
            calls.incrementAndGet();
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("stub interrupted", e);
                }
            }
            if (throwInstead) {
                throw new IllegalStateException("model:" + text + " failed");
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }
    }

    private static String textOf(ChatResponse response) {
        return response.getResult().getOutput().getText();
    }

    @Test
    void slowPrimaryFastHedgeWinsQuickly() {
        StubModel primary = new StubModel("primary", 5_000, false);
        StubModel backup = new StubModel("backup", 20, false);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            HedgedChatModel hedged = new HedgedChatModel(
                    primary, backup, Duration.ofMillis(150), pool);

            long start = System.nanoTime();
            String out = textOf(hedged.call(new Prompt(List.of())));
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            assertThat(out).isEqualTo("backup");
            // 对冲生效：总耗时 ≈ delay(150) + 快备(20)，远小于主模型 5s
            assertThat(elapsedMs).isLessThan(1_500);
            assertThat(primary.calls.get()).isEqualTo(1); // 主确被调用（后被取消）
        }
    }

    @Test
    void fastPrimarySkipsHedgeEntirely() {
        StubModel primary = new StubModel("primary", 10, false);
        StubModel backup = new StubModel("backup", 10, false);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            HedgedChatModel hedged = new HedgedChatModel(
                    primary, backup, Duration.ofMillis(500), pool);

            assertThat(textOf(hedged.call(new Prompt(List.of())))).isEqualTo("primary");
            assertThat(backup.calls.get()).isZero(); // 未触发对冲——零双倍成本
        }
    }

    @Test
    void primaryFastFailureDefersToHedge() {
        StubModel primary = new StubModel("primary", 0, true);
        StubModel backup = new StubModel("backup", 20, false);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            HedgedChatModel hedged = new HedgedChatModel(
                    primary, backup, Duration.ofSeconds(5), pool);

            assertThat(textOf(hedged.call(new Prompt(List.of())))).isEqualTo("backup");
        }
    }

    @Test
    void bothFailThrowsPrimaryException() {
        StubModel primary = new StubModel("primary", 0, true);
        StubModel backup = new StubModel("backup", 0, true);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            HedgedChatModel hedged = new HedgedChatModel(
                    primary, backup, Duration.ofMillis(50), pool);

            assertThatThrownBy(() -> hedged.call(new Prompt(List.of())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("primary failed"); // 主异常——降级链口径不变
        }
    }

    @Test
    void streamDelegatesToPrimaryWithoutTouchingHedge() {
        StubModel primary = new StubModel("primary", 0, false);
        StubModel backup = new StubModel("backup", 0, false);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            HedgedChatModel hedged = new HedgedChatModel(
                    primary, backup, Duration.ofMillis(50), pool);

            String streamed = hedged.stream(new Prompt(List.of()))
                    .blockLast().getResult().getOutput().getText();
            assertThat(streamed).isEqualTo("primary");
            assertThat(backup.seenPrompts).isEmpty(); // 流不打扰备
        }
    }

    @Test
    void constructorValidatesArguments() {
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            StubModel m = new StubModel("m", 0, false);
            assertThatThrownBy(() -> new HedgedChatModel(null, m,
                    Duration.ofMillis(1), pool)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new HedgedChatModel(m, m,
                    Duration.ZERO, pool)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
