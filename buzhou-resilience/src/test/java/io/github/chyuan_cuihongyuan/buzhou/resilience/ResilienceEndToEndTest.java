package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.advisor.ResilienceAdvisor;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 韧性层端到端测试（主缝合点：经 {@code Buzhou.runtime(...)} 装配完整 advisor 链后 {@code spawn().chat()}，
 * 断言最终回复 + observability 事件流）。对齐 {@code HookEndToEndTest} 的 e2e 形态。
 */
class ResilienceEndToEndTest {

    // ---- 01：瞬时网络错误重试 ----

    /** 瞬时网络错误一次后成功：韧性层重试后用户拿到正确回复，且模型恰好被调用 2 次（无双倍重试）。 */
    @Test
    void transientNetworkErrorRetriedThenSucceeds() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("connection reset"));
        model.enqueue(new AssistantMessage("ok"));
        ResilienceProperties props = fastBackoff(3);

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        String reply = session.chat("hi");

        assertThat(reply).isEqualTo("ok");
        assertThat(model.seenPrompts).hasSize(2); // 1 失败 + 1 成功，未被底座双倍重试
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_RETRY_ATTEMPTED.equals(e.type())
                && "NETWORK".equals(e.payload().get("category")));
        session.close();
    }

    /** 重试耗尽：maxAttempts 用尽后原异常按底座语义向上抛，retry-exhausted 事件上报。 */
    @Test
    void retryExhaustedRethrowsAfterMaxAttempts() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("boom-1"));
        model.enqueueThrow(networkError("boom-2"));
        ResilienceProperties props = fastBackoff(2);

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        assertThatThrownBy(() -> session.chat("hi")).isInstanceOf(UncheckedIOException.class);
        assertThat(model.seenPrompts).hasSize(2);
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_RETRY_EXHAUSTED.equals(e.type())
                && "NETWORK".equals(e.payload().get("category")));
        session.close();
    }

    /** enabled=false：回退底座原生行为，瞬时错误立即抛出、无重试事件。 */
    @Test
    void disabledFallsBackToBaseBehavior() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("boom"));
        model.enqueue(new AssistantMessage("ok"));
        ResilienceProperties props = new ResilienceProperties(false, 3,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null);

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        assertThatThrownBy(() -> session.chat("hi")).isInstanceOf(UncheckedIOException.class);
        assertThat(model.seenPrompts).hasSize(1); // 不重试
        assertThat(events).noneMatch(e -> e.type().startsWith("retry-"));
        session.close();
    }

    /** 未知错误默认不重试（保守）：立即抛出、无重试事件。 */
    @Test
    void unknownErrorNotRetried() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(new IllegalStateException("weird provider glitch"));
        model.enqueue(new AssistantMessage("ok"));
        ResilienceProperties props = fastBackoff(3);

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        assertThatThrownBy(() -> session.chat("hi")).isInstanceOf(IllegalStateException.class);
        assertThat(model.seenPrompts).hasSize(1);
        assertThat(events).noneMatch(e -> e.type().startsWith("retry-"));
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_ERROR_CLASSIFIED.equals(e.type())
                && "UNKNOWN".equals(e.payload().get("category")));
        session.close();
    }

    /** max-attempts 经参数生效：前两次失败、第三次成功，模型恰好被调用 3 次。 */
    @Test
    void maxAttemptsConfigurableAllowsThirdAttemptSuccess() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(networkError("boom-1"));
        model.enqueueThrow(networkError("boom-2"));
        model.enqueue(new AssistantMessage("recovered"));
        ResilienceProperties props = fastBackoff(3);

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        assertThat(session.chat("hi")).isEqualTo("recovered");
        assertThat(model.seenPrompts).hasSize(3);
        assertThat(events).filteredOn(e -> ResilienceAdvisor.EVENT_RETRY_ATTEMPTED.equals(e.type())).hasSize(2);
        session.close();
    }

    // ---- 02：五类分类 / Retry-After / 内容拒绝 / 可重试覆盖 ----

    /** 429 限流被重试，Retry-After 被解析并钳制到 maxBackoff（retryAfter=true 标记尊重服务端建议）。 */
    @Test
    void rateLimit429RetriedWithRetryAfterClamped() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(httpError(429, "100")); // Retry-After=100s，会被钳制到 maxBackoff
        model.enqueue(new AssistantMessage("ok"));
        // maxBackoff=20ms：Retry-After(100s) 钳到 20ms，测试快且证明 Retry-After 价值流向退避。
        ResilienceProperties props = new ResilienceProperties(true, 3,
                Duration.ofMillis(1), Duration.ofMillis(20), 2.0, 0.0, null, null);

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        assertThat(session.chat("hi")).isEqualTo("ok");
        assertThat(model.seenPrompts).hasSize(2);
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_RETRY_ATTEMPTED.equals(e.type())
                && "RATE_LIMIT".equals(e.payload().get("category"))
                && Boolean.TRUE.equals(e.payload().get("retryAfter"))
                && Long.valueOf(20L).equals(e.payload().get("backoffMs")));
        session.close();
    }

    /** 5xx 服务端瞬时故障归 NETWORK、被重试。 */
    @Test
    void serverError5xxClassifiedAndRetried() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(httpError(503, null));
        model.enqueue(new AssistantMessage("ok"));
        ResilienceProperties props = fastBackoff(3);

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        assertThat(session.chat("hi")).isEqualTo("ok");
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_ERROR_CLASSIFIED.equals(e.type())
                && "NETWORK".equals(e.payload().get("category")));
        session.close();
    }

    /** 401 鉴权失败归 AUTH、不重试、快速失败。 */
    @Test
    void authErrorNotRetriedFastFails() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(httpError(401, null));
        model.enqueue(new AssistantMessage("ok"));
        ResilienceProperties props = fastBackoff(3);

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        assertThatThrownBy(() -> session.chat("hi")).isInstanceOf(HttpClientErrorException.class);
        assertThat(model.seenPrompts).hasSize(1);
        assertThat(events).noneMatch(e -> e.type().startsWith("retry-"));
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_ERROR_CLASSIFIED.equals(e.type())
                && "AUTH".equals(e.payload().get("category")));
        session.close();
    }

    /** 内容拒绝（静默通道）被识别为 CONTENT、不重试、content-refusal-detected + error-classified 上报。 */
    @Test
    void contentRefusalDetectedNotRetried() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.script.add(contentRefusalResponse());
        ResilienceProperties props = fastBackoff(3);

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        session.chat("hi"); // 不抛异常（静默拒绝）
        assertThat(model.seenPrompts).hasSize(1); // 不重试
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_CONTENT_REFUSAL_DETECTED.equals(e.type()));
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_ERROR_CLASSIFIED.equals(e.type())
                && "CONTENT".equals(e.payload().get("category")));
        session.close();
    }

    /** retryable-categories 覆盖默认表：把 UNKNOWN 配为重试后，未知错误也被重试。 */
    @Test
    void retryableCategoriesOverrideRetriesUnknown() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueThrow(new IllegalStateException("weird"));
        model.enqueue(new AssistantMessage("ok"));
        ResilienceProperties props = new ResilienceProperties(true, 3,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, List.of("UNKNOWN"), null);

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        assertThat(session.chat("hi")).isEqualTo("ok");
        assertThat(model.seenPrompts).hasSize(2);
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_RETRY_ATTEMPTED.equals(e.type())
                && "UNKNOWN".equals(e.payload().get("category")));
        session.close();
    }

    // ---- 03：统一超时（deadline）+ session.cancel() 在途模型调用漏网修复 ----

    /** 慢模型 + 短 deadline：deadline 生效、发出 timeout-fired、在途调用被中断（不用 wall-clock sleep 在断言侧）。 */
    @Test
    void deadlineTimeoutFiresAndCancelsInFlightCall() throws Exception {
        BlockingChatModel model = BlockingChatModel.neverReturns(); // 阻塞，直到被中断
        ResilienceProperties props = new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, Duration.ofMillis(50));
        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        long start = System.nanoTime();
        assertThatThrownBy(() -> session.chat("hi")).isInstanceOf(RuntimeException.class);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isLessThan(2000); // 远小于模型阻塞时长，证明 deadline 兜底而非等模型自愈
        assertThat(events).anyMatch(e -> ResilienceAdvisor.EVENT_TIMEOUT_FIRED.equals(e.type()));
        assertThat(model.interruptedLatch.await(2, TimeUnit.SECONDS)).isTrue();
        session.close();
    }

    /** session.cancel() 在模型调用进行中能中断它（修复「只中断工具、不触及模型」的漏网）。 */
    @Test
    void sessionCancelInterruptsInFlightModelCall() throws Exception {
        BlockingChatModel model = BlockingChatModel.neverReturns();
        ResilienceProperties props = new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, Duration.ofSeconds(10));
        AgentSession session = newRuntime(model, props);

        ExecutorService testExec = Executors.newVirtualThreadPerTaskExecutor();
        try {
            Future<?> chatFuture = testExec.submit(() -> session.chat("hi"));
            assertThat(model.startedLatch.await(2, TimeUnit.SECONDS)).isTrue(); // 模型已进入在途阻塞
            session.cancel();
            assertThatThrownBy(() -> chatFuture.get(5, TimeUnit.SECONDS)).isNotNull(); // chat() 终止（抛或返回异常）
            assertThat(model.interruptedLatch.await(2, TimeUnit.SECONDS))
                    .as("在途模型调用被 session.cancel() 中断").isTrue();
        } finally {
            testExec.shutdownNow();
            session.close();
        }
    }

    /** deadline 经 yml 可调且生效：短 deadline 命中、长 deadline 放行。 */
    @Test
    void deadlineConfigurable() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("ok"));
        ResilienceProperties props = new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, Duration.ofSeconds(5));
        AgentSession session = newRuntime(model, props);

        assertThat(session.chat("hi")).isEqualTo("ok"); // 模型即时返回，5s deadline 不触发
        session.close();
    }

    // ---- helpers ----

    private static AgentSession newRuntime(ScriptedChatModel model, ResilienceProperties props) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, ResilienceModule.configure(props));
        return runtime.spawn("app", "resilience-agent", "sess-" + System.nanoTime());
    }

    private static List<SessionEvent> listen(AgentSession session) {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        return events;
    }

    /** 小退避、关抖动、默认可重试表 {RATE_LIMIT, NETWORK}：让重试测试快且确定。 */
    private static ResilienceProperties fastBackoff(int maxAttempts) {
        return new ResilienceProperties(true, maxAttempts,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null);
    }

    /** 一个会被默认分类器归入 NETWORK 的瞬时错误（类名含 IOException 子串）。 */
    private static UncheckedIOException networkError(String message) {
        return new UncheckedIOException(new IOException(message));
    }

    /** 构造一个带 HTTP 状态（+ 可选 Retry-After 头）的 RestClient 系异常，模拟 provider 错误响应。 */
    private static HttpClientErrorException httpError(int status, String retryAfterSeconds) {
        HttpHeaders headers = new HttpHeaders();
        if (retryAfterSeconds != null) {
            headers.add(HttpHeaders.RETRY_AFTER, retryAfterSeconds);
        }
        return HttpClientErrorException.create(HttpStatusCode.valueOf(status), "provider error",
                headers, new byte[0], StandardCharsets.UTF_8);
    }

    /** 构造一个内容拒绝响应：finishReason=content_filter（静默，不抛异常）。 */
    private static ChatResponse contentRefusalResponse() {
        ChatGenerationMetadata metadata = ChatGenerationMetadata.builder().finishReason("content_filter").build();
        return new ChatResponse(List.of(new Generation(new AssistantMessage(""), metadata)));
    }

    /**
     * 阻塞型 ChatModel：{@code call} 进入即标记 started，随后阻塞在内部 latch 上，直到被中断
     * （deadline 超时的 cancel(true) 或 session.cancel()）。对齐 {@code HarnessToolCallingManagerTest}
     * 的 latch 手法——断言侧不用 wall-clock sleep。
     */
    static final class BlockingChatModel extends ScriptedChatModel {
        final CountDownLatch startedLatch = new CountDownLatch(1);
        final CountDownLatch interruptedLatch = new CountDownLatch(1);
        private final CountDownLatch proceed;

        private BlockingChatModel(CountDownLatch proceed) {
            this.proceed = proceed;
        }

        static BlockingChatModel neverReturns() {
            return new BlockingChatModel(new CountDownLatch(1)); // 永不 countDown，只能被中断
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            seenPrompts.add(prompt);
            startedLatch.countDown();
            try {
                proceed.await();
            } catch (InterruptedException e) {
                interruptedLatch.countDown();
                Thread.currentThread().interrupt();
                throw new RuntimeException("model call interrupted");
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("late"))));
        }
    }
}
