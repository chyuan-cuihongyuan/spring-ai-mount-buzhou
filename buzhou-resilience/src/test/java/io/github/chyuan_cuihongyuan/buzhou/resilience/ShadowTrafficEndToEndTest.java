package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowTrafficController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 49 §A / T176 / impl-145：shadow 探测端到端——对照事件、护栏（并发/预算）、失败吞噬、
 * 默认关零变化、用户路径零影响。
 */
class ShadowTrafficEndToEndTest {

    private final List<String> counters = new CopyOnWriteArrayList<>();

    @AfterEach
    void reset() {
        BuzhouMetricsHolder.reset();
    }

    private void installCapturingMetrics() {
        BuzhouMetricsHolder.install(new BuzhouMetrics() {
            @Override
            public void counter(String name, long delta, String... tagKeyValue) {
                if ("buzhou.resilience.shadow.calls".equals(name) && tagKeyValue.length >= 2) {
                    counters.add(tagKeyValue[1]);
                }
            }

            @Override
            public void timer(String name, Duration duration, String... tagKeyValue) {
            }
        });
    }

    /** 主成功 → shadow 对照事件（primary/shadow 延迟与差值）+ ok 计数 + 用户回复不受影响。 */
    @Test
    void successfulPrimaryTriggersShadowComparison() throws Exception {
        installCapturingMetrics();
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("from-primary"));
        ScriptedChatModel shadow = new ScriptedChatModel();
        shadow.enqueue(new AssistantMessage("from-shadow"));
        SessionFixture fx = spawn(primary, shadow, shadowProps(null, null));

        String reply = fx.session.chat("hi");
        assertThat(reply).isEqualTo("from-primary"); // 用户路径零影响

        awaitEvent(fx.events, ShadowTrafficController.EVENT_COMPARED);
        SessionEvent compared = fx.events.stream()
                .filter(e -> ShadowTrafficController.EVENT_COMPARED.equals(e.type()))
                .reduce((a, b) -> b).orElseThrow();
        assertThat(compared.payload()).containsEntry("primary", "primary")
                .containsEntry("shadow", "shadow-model")
                .containsKeys("primaryMs", "shadowMs", "deltaMs");
        assertThat(counters).contains("ok");
        fx.session.close();
    }

    /** 日预算 = 1：第二次提交计 skipped-budget、无第二个对照事件。 */
    @Test
    void budgetExhaustionSkipsSecondSubmission() throws Exception {
        installCapturingMetrics();
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("r1"));
        primary.enqueue(new AssistantMessage("r2"));
        ScriptedChatModel shadow = new ScriptedChatModel();
        shadow.enqueue(new AssistantMessage("s1"));
        shadow.enqueue(new AssistantMessage("s2"));
        SessionFixture fx = spawn(primary, shadow, shadowProps(1L, null));

        assertThat(fx.session.chat("q1")).isEqualTo("r1");
        awaitEvent(fx.events, ShadowTrafficController.EVENT_COMPARED);
        assertThat(fx.session.chat("q2")).isEqualTo("r2");
        Thread.sleep(150); // 预算尽：第二回合无新对照事件
        long events = fx.events.stream()
                .filter(e -> ShadowTrafficController.EVENT_COMPARED.equals(e.type())).count();
        assertThat(events).isEqualTo(1);
        assertThat(counters).contains("skipped-budget");
        fx.session.close();
    }

    /** 并发 = 1 + 慢 shadow（闭锁挂住）：并发第二次提交计 skipped-concurrency。 */
    @Test
    void concurrencyCapSkipsOverflowSubmission() throws Exception {
        installCapturingMetrics();
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("r1"));
        primary.enqueue(new AssistantMessage("r2"));
        CountDownLatch holdShadow = new CountDownLatch(1);
        AtomicInteger shadowCalls = new AtomicInteger();
        ChatModel slowShadow = new ChatModel() {
            @Override
            public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
                return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
            }

            @Override
            public ChatResponse call(Prompt prompt) {
                shadowCalls.incrementAndGet();
                try {
                    holdShadow.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new ChatResponse(List.of(new Generation(new AssistantMessage("slow"))));
            }

            @Override
            public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
                return reactor.core.publisher.Flux.just(call(prompt));
            }
        };
        SessionFixture fx = spawn(primary, slowShadow, shadowProps(null, 1));

        assertThat(fx.session.chat("q1")).isEqualTo("r1");
        awaitShadowStarted(shadowCalls);
        assertThat(fx.session.chat("q2")).isEqualTo("r2");
        Thread.sleep(150);
        assertThat(counters).contains("skipped-concurrency");
        holdShadow.countDown();
        fx.session.close();
    }

    /** shadow 抛错 → outcome=error 计数、主链路回复照常、无异常外溢。 */
    @Test
    void shadowFailureSwallowed() throws Exception {
        installCapturingMetrics();
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("fine"));
        ScriptedChatModel shadow = new ScriptedChatModel();
        shadow.enqueueThrow(new UncheckedIOException(new IOException("shadow down")));
        SessionFixture fx = spawn(primary, shadow, shadowProps(null, null));

        assertThat(fx.session.chat("hi")).isEqualTo("fine");
        Thread.sleep(200);
        assertThat(counters).contains("error");
        assertThat(fx.events.stream()
                .filter(e -> ShadowTrafficController.EVENT_COMPARED.equals(e.type())).count()).isZero();
        fx.session.close();
    }

    /** 默认关（未配置 shadow）：零事件零计数。 */
    @Test
    void disabledByDefaultZeroShadowActivity() throws Exception {
        installCapturingMetrics();
        ScriptedChatModel primary = new ScriptedChatModel();
        primary.enqueue(new AssistantMessage("solo"));
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                plainProps(), "primary", new ResilienceStats()));
        AgentSession session = runtime.spawn("app", "agent", "shadow-off");
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);

        assertThat(session.chat("hi")).isEqualTo("solo");
        Thread.sleep(100);
        assertThat(events).noneMatch(e -> ShadowTrafficController.EVENT_COMPARED.equals(e.type()));
        assertThat(counters).isEmpty();
        session.close();
    }

    // ---- helpers ----

    private record SessionFixture(AgentSession session, List<SessionEvent> events) {
    }

    private static ResilienceProperties plainProps() {
        return new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), null, null, null, null, null);
    }

    private static ResilienceProperties shadowProps(Long dailyBudget, Integer maxConcurrent) {
        return new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), null, null, null, null,
                new ResilienceProperties.Shadow(Boolean.TRUE, null, maxConcurrent, dailyBudget));
    }

    private static SessionFixture spawn(ScriptedChatModel primary, ChatModel shadow,
            ResilienceProperties props) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(primary, stores, ResilienceModule.configure(
                props, "primary", new ResilienceStats(), null,
                List.of(new NamedFallbackModel("shadow-model", shadow))));
        AgentSession session = runtime.spawn("app", "agent", "sess-shadow-" + System.nanoTime());
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        return new SessionFixture(session, events);
    }

    private static void awaitEvent(List<SessionEvent> events, String type) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (events.stream().noneMatch(e -> type.equals(e.type()))
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertThat(events.stream().filter(e -> type.equals(e.type())).count())
                .as("事件 " + type + " 在 5s 内到达").isPositive();
    }

    private static void awaitShadowStarted(AtomicInteger calls) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (calls.get() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
    }
}
