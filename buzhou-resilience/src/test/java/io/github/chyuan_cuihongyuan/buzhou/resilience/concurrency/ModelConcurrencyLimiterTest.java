package io.github.chyuan_cuihongyuan.buzhou.resilience.concurrency;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 426 §Testing / T743–T744：模型并发舱——limiter cap/NOOP/观测/超时；
 * advisor E2E 阻塞双线程（A 持 B 拒）、流式完全消费后许可已还、流中途
 * dispose（CANCEL）也还；yml limits 装配/缺席。
 */
class ModelConcurrencyLimiterTest {

    @Test
    void shouldEnforceCapWithFailFastAndObserveInFlight() {
        ModelConcurrencyLimiter limiter = new ModelConcurrencyLimiter(
                Map.of("gpt-4o", 2), null);

        limiter.acquireOrThrow("gpt-4o");
        limiter.acquireOrThrow("gpt-4o");
        assertThat(limiter.inFlight()).containsEntry("gpt-4o", 2);

        // fail-fast（默认 timeout 0）：第三取即拒
        assertThatThrownBy(() -> limiter.acquireOrThrow("gpt-4o"))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("模型并发舱已满");

        limiter.release("gpt-4o");
        assertThat(limiter.inFlight()).containsEntry("gpt-4o", 1);
        limiter.acquireOrThrow("gpt-4o"); // 空位可再取

        // 未配置模型 = NOOP（不限不记）
        limiter.acquireOrThrow("claude-3");
        assertThat(limiter.inFlight()).doesNotContainKey("claude-3");
    }

    @Test
    void shouldTimeoutWhenPermitHeldWithAcquireTimeout() {
        ModelConcurrencyLimiter limiter = new ModelConcurrencyLimiter(
                Map.of("m", 1), Duration.ofMillis(50));
        limiter.acquireOrThrow("m");
        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> limiter.acquireOrThrow("m"))
                .isInstanceOf(BuzhouException.class);
        assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(40);
    }

    /** 阻塞模型（call 等闩）——E2E 并发占用真发生。 */
    private static final class BlockingChatModel extends ScriptedChatModel {
        final CountDownLatch releaseGate = new CountDownLatch(1);
        final CountDownLatch entered = new CountDownLatch(1);
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public ChatResponse call(Prompt prompt) {
            calls.incrementAndGet();
            entered.countDown();
            try {
                releaseGate.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return super.call(prompt);
        }
    }

    private static RuntimeConfig configWith(ModelConcurrencyLimiter limiter) {
        return new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                null, List.of(), Map.of(), List.of(),
                List.of(ctx -> ctx.addAdvisor(new ModelConcurrencyAdvisor(limiter, "gpt-4o"))),
                null);
    }

    @Test
    void shouldRejectSecondInFlightCall_andAdmitAfterRelease() throws Exception {
        ModelConcurrencyLimiter limiter = new ModelConcurrencyLimiter(Map.of("gpt-4o", 1), null);
        BlockingChatModel model = new BlockingChatModel();
        var runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), configWith(limiter));
        var sessionA = runtime.spawn("app", "ag", "s1");
        var sessionB = runtime.spawn("app", "ag", "s2"); // 同 runtime 不同会话——单飞闸不拦
        try {
            CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> sessionA.chat("慢问题"));
            assertThat(model.entered.await(5, TimeUnit.SECONDS)).isTrue(); // A 已进入（持许可）

            // cap=1：B 拒（QUOTA_EXCEEDED——fail-fast 不排队）
            assertThatThrownBy(() -> sessionB.chat("快问题"))
                    .isInstanceOf(BuzhouException.class)
                    .hasMessageContaining("模型并发舱已满");
            assertThat(model.calls.get()).isEqualTo(1); // B 没打到模型

            model.releaseGate.countDown(); // A 放行 → finally 归还许可
            assertThat(first.get(5, TimeUnit.SECONDS)).isNotNull();
            assertThat(sessionB.chat("后续问题")).isNotNull(); // 许可已还可再进
        } finally {
            sessionA.close();
            sessionB.close();
        }
    }

    @Test
    void shouldReleasePermitAfterStreamCompletes_andAfterCancel() throws Exception {
        ModelConcurrencyLimiter limiter = new ModelConcurrencyLimiter(Map.of("gpt-4o", 1), null);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("流式回复");
        try (var agent = Buzhou.runtime(model, Buzhou.inMemoryStores(), configWith(limiter))
                .spawn("app", "ag", "s2")) {
            // 流完全消费 → doFinally 归还
            var sb = agent.stream("流式问题").collectList().block(Duration.ofSeconds(5));
            assertThat(sb).isNotNull();
            assertThat(limiter.inFlight()).containsEntry("gpt-4o", 0);

            // 流中途 dispose（CANCEL）→ doFinally 同样归还
            model.enqueueText("会被取消的流");
            var disposable = agent.stream("取消问题").subscribe();
            disposable.dispose();
            // CANCEL 信号异步传播——轮询等归还
            long deadline = System.currentTimeMillis() + 5_000;
            while (limiter.inFlight().get("gpt-4o") != 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            assertThat(limiter.inFlight()).containsEntry("gpt-4o", 0);
        }
    }

    @Test
    void shouldAssembleOnlyWhenLimitsDeclared() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.resilience.config
                                .BuzhouResilienceAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.resilience.model-concurrency.limits.gpt-4o=4",
                        "buzhou.resilience.model-concurrency.acquire-timeout=100ms")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("modelConcurrencyRuntimeConfig");
                    // spec 429：三 bean 齐（limiter 恒 exposed + 热更新共享实例）
                    assertThat(context).hasBean("buzhouModelConcurrencyLimiter");
                    assertThat(context).hasBean("buzhouModelConcurrencyHotReload");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.resilience.config
                                .BuzhouResilienceAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("modelConcurrencyRuntimeConfig");
                    assertThat(context).doesNotHaveBean("buzhouModelConcurrencyLimiter");
                    assertThat(context).doesNotHaveBean("buzhouModelConcurrencyHotReload");
                });
    }

    @Test
    void shouldResizeGrowShrinkAndRemoveWithoutDisturbingInFlight() {
        ModelConcurrencyLimiter limiter = new ModelConcurrencyLimiter(Map.of("m", 1), null);

        // 扩容：cap 1 → 2（在飞 1 不受扰，第二取成功）
        limiter.acquireOrThrow("m");
        limiter.resize(Map.of("m", 2));
        limiter.acquireOrThrow("m");
        assertThat(limiter.inFlight()).containsEntry("m", 2);

        // 缩容低于在飞：2 → 1（在飞 2 瞬时共存）→ 新取拒 → 释放两个后恢复
        limiter.resize(Map.of("m", 1));
        assertThatThrownBy(() -> limiter.acquireOrThrow("m")).isInstanceOf(BuzhouException.class);
        limiter.release("m");
        assertThatThrownBy(() -> limiter.acquireOrThrow("m")).isInstanceOf(BuzhouException.class);
        limiter.release("m"); // 释放到限内 → 自然收敛
        limiter.acquireOrThrow("m");
        assertThat(limiter.inFlight()).containsEntry("m", 1);

        // 摘舱：移除键 → 新 acquire NOOP（在飞释放无害）
        limiter.release("m");
        limiter.resize(Map.of());
        limiter.acquireOrThrow("m"); // NOOP 不抛
        assertThat(limiter.inFlight()).isEmpty();
    }

    @Test
    void shouldHotReloadLimitsOnRefreshEvent() {
        ModelConcurrencyLimiter limiter = new ModelConcurrencyLimiter(Map.of("m", 1), null);
        org.springframework.core.env.StandardEnvironment env =
                new org.springframework.core.env.StandardEnvironment();
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put("buzhou.resilience.model-concurrency.limits.m", "3");
        org.springframework.core.env.MapPropertySource source =
                new org.springframework.core.env.MapPropertySource("hot", props);
        env.getPropertySources().addFirst(source);

        ModelConcurrencyHotReload hotReload = new ModelConcurrencyHotReload(limiter, env);
        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent event =
                new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent(this);

        hotReload.onApplicationEvent(event); // 第一跳：cap 1 → 3
        limiter.acquireOrThrow("m");
        limiter.acquireOrThrow("m");
        limiter.acquireOrThrow("m");
        assertThatThrownBy(() -> limiter.acquireOrThrow("m")).isInstanceOf(BuzhouException.class);

        props.put("buzhou.resilience.model-concurrency.limits.m", "1");
        hotReload.onApplicationEvent(event); // 第二跳：3 → 1（在飞 3 共存，新取拒）
        assertThatThrownBy(() -> limiter.acquireOrThrow("m")).isInstanceOf(BuzhouException.class);
        assertThat(hotReload.reloadCount()).isEqualTo(2);
    }
}
