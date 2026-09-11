package io.github.chyuan_cuihongyuan.buzhou.resilience.idempotency;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 501 / T753–T754：请求幂等键——同键重放零二次调用、异键/无键透传、
 * 非终态不写、流式单元素重放、yml 装配缺席（Stripe Idempotency-Key 借鉴）。
 */
class IdempotencyAdvisorTest {

    /** 内联链：第 N 次调用返回脚本第 N 条回复（StructuredOutputTest 同法）。 */
    private static final class StubChain implements CallAdvisorChain {
        final List<String> replies;
        final List<ChatClientRequest> seen = new CopyOnWriteArrayList<>();
        int cursor;

        StubChain(List<String> replies) {
            this.replies = replies;
        }

        private ChatClientResponse consume(ChatClientRequest request) {
            seen.add(request);
            String reply = replies.get(Math.min(cursor, replies.size() - 1));
            cursor++;
            return new ChatClientResponse(new ChatResponse(
                    List.of(new Generation(new AssistantMessage(reply)))), request.context());
        }

        @Override
        public ChatClientResponse nextCall(ChatClientRequest request) {
            return consume(request);
        }

        @Override
        public List<org.springframework.ai.chat.client.advisor.api.CallAdvisor> getCallAdvisors() {
            return List.of();
        }

        @Override
        public CallAdvisorChain copy(org.springframework.ai.chat.client.advisor.api.CallAdvisor advisor) {
            throw new UnsupportedOperationException();
        }
    }

    /** 流式桩链：记录被订阅次数；每次订阅产出脚本 chunk。 */
    private static final class StubStreamChain implements StreamAdvisorChain {
        final List<String> chunks;
        final AtomicInteger subscriptions = new AtomicInteger();

        StubStreamChain(List<String> chunks) {
            this.chunks = chunks;
        }

        @Override
        public Flux<ChatClientResponse> nextStream(ChatClientRequest request) {
            subscriptions.incrementAndGet();
            return Flux.fromIterable(chunks).map(text -> new ChatClientResponse(new ChatResponse(
                    List.of(new Generation(new AssistantMessage(text)))), request.context()));
        }

        @Override
        public List<org.springframework.ai.chat.client.advisor.api.StreamAdvisor> getStreamAdvisors() {
            return List.of();
        }

        @Override
        public StreamAdvisorChain copy(org.springframework.ai.chat.client.advisor.api.StreamAdvisor advisor) {
            throw new UnsupportedOperationException();
        }
    }

    private static ChatClientRequest request(String userText, String idempotencyKey) {
        Map<String, Object> context = idempotencyKey == null
                ? Map.of()
                : Map.of(IdempotencyAdvisor.IDEMPOTENCY_KEY_PARAM, idempotencyKey);
        return new ChatClientRequest(new Prompt(List.of(new UserMessage(userText))), context);
    }

    private static String textOf(ChatClientResponse response) {
        return response.chatResponse().getResult().getOutput().getText();
    }

    @Test
    void replaysFirstResponseOnSameKeyWithoutSecondModelCall() {
        IdempotencyAdvisor advisor = new IdempotencyAdvisor(
                new io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheStore(
                        16, Duration.ofMinutes(5)));
        StubChain chain = new StubChain(List.of("首次答案"));
        String first = textOf(advisor.adviseCall(request("问", "sess:1"), chain));
        String replayed = textOf(advisor.adviseCall(request("问", "sess:1"), chain));

        assertThat(first).isEqualTo("首次答案");
        assertThat(replayed).isEqualTo("首次答案");
        assertThat(chain.seen).hasSize(1); // 第二次零链调用（零模型调用）
        assertThat(advisor.store().hitCount()).isEqualTo(1);
    }

    @Test
    void differentKeysCallThroughIndependently() {
        IdempotencyAdvisor advisor = new IdempotencyAdvisor(
                new io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheStore(
                        16, Duration.ofMinutes(5)));
        StubChain chain = new StubChain(List.of("答案一", "答案二"));
        assertThat(textOf(advisor.adviseCall(request("问", "k1"), chain))).isEqualTo("答案一");
        assertThat(textOf(advisor.adviseCall(request("问", "k2"), chain))).isEqualTo("答案二");
        assertThat(chain.seen).hasSize(2);
    }

    @Test
    void missingKeyPassesThroughEveryTime() {
        IdempotencyAdvisor advisor = new IdempotencyAdvisor(
                new io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheStore(
                        16, Duration.ofMinutes(5)));
        StubChain chain = new StubChain(List.of("a", "b"));
        assertThat(textOf(advisor.adviseCall(request("问", null), chain))).isEqualTo("a");
        assertThat(textOf(advisor.adviseCall(request("问", null), chain))).isEqualTo("b");
        assertThat(chain.seen).hasSize(2);
        assertThat(advisor.store().hitCount()).isZero();
    }

    @Test
    void nonTerminalResponseIsNeverStored() {
        IdempotencyAdvisor advisor = new IdempotencyAdvisor(
                new io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheStore(
                        16, Duration.ofMinutes(5)));
        StubChain chain = new StubChain(List.of("")); // 空内容 = 非终态
        assertThat(textOf(advisor.adviseCall(request("问", "k"), chain))).isEmpty();
        assertThat(textOf(advisor.adviseCall(request("问", "k"), chain))).isEmpty();
        assertThat(chain.seen).hasSize(2); // 未写——同键第二次仍真调
    }

    @Test
    void streamReplayEmitsAggregatedTextWithoutResubscribing() {
        IdempotencyAdvisor advisor = new IdempotencyAdvisor(
                new io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheStore(
                        16, Duration.ofMinutes(5)));
        StubStreamChain first = new StubStreamChain(List.of("你", "好"));
        advisor.adviseStream(request("问", "k9"), first).blockLast();
        assertThat(first.subscriptions.get()).isEqualTo(1);

        StubStreamChain second = new StubStreamChain(List.of("不", "应", "再", "调"));
        List<String> replayed = advisor.adviseStream(request("问", "k9"), second)
                .map(IdempotencyAdvisorTest::textOf)
                .collectList()
                .block();

        assertThat(replayed).containsExactly("你好"); // 单元素重放聚合文本
        assertThat(second.subscriptions.get()).isZero(); // 桩链未被订阅
    }

    @Test
    void ymlAssemblyOnlyWhenEnabled() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.resilience.config
                                .BuzhouResilienceAutoConfiguration.class))
                .withPropertyValues("buzhou.resilience.idempotency.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouIdempotencyStore");
                    assertThat(context).hasBean("idempotencyRuntimeConfig");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.resilience.config
                                .BuzhouResilienceAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouIdempotencyStore");
                    assertThat(context).doesNotHaveBean("idempotencyRuntimeConfig");
                });
    }
}
