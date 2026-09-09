package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.PriorityLane;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 422 §Testing / T735–T736：优先级泳道工具装配——高优先级插队（逐次
 * release+完成屏障——R12 教训）、超时结构化异常、异常路径许可归还、
 * registry 命名单例、yml 装配/缺席/未知泳道启动红、E2E。
 */
class PriorityLaneToolCallbackTest {

    private static ToolCallback tool(String name) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                        .name(name).description("测试工具").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                return "ok:" + toolInput;
            }
        };
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("条件等待超时");
            }
            Thread.sleep(10);
        }
    }

    @Test
    void shouldLetHighPriorityToolJumpQueue() throws Exception {
        PriorityLane lane = new PriorityLane(1);
        lane.acquire(0, Duration.ofSeconds(1)); // 主线程持走唯一许可

        // 完成序记录（1 许可串行化执行——先拿许可者先执行；R12 教训：join 返回时
        // 高优先级 finally 已把许可交给低优先级，isNotDone 型断言必输给竞争，
        // 断言目标改为执行顺序本身）
        java.util.List<String> order = new java.util.concurrent.CopyOnWriteArrayList<>();
        ToolCallback lowTool = recordingTool("batch_report", "low", order);
        ToolCallback highTool = recordingTool("interactive_lookup", "high", order);

        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<String> low = CompletableFuture.supplyAsync(
                    () -> new PriorityLaneToolCallback(lowTool, lane, 5, Duration.ofSeconds(30)).call("x"), pool);
            awaitTrue(() -> lane.waitingByPriority().getOrDefault(5, 0) == 1); // 低优先级已排队

            CompletableFuture<String> high = CompletableFuture.supplyAsync(
                    () -> new PriorityLaneToolCallback(highTool, lane, 1, Duration.ofSeconds(30)).call("y"), pool);
            awaitTrue(() -> lane.waitingByPriority().getOrDefault(1, 0) == 1); // 高优先级后到已排队

            lane.release(); // 唯一许可有向交接给队首=高优先级
            assertThat(high.join()).isEqualTo("ok:y");
            assertThat(low.join()).isEqualTo("ok:x");
            assertThat(order).containsExactly("high", "low"); // 高先执行——插队语义
        } finally {
            pool.shutdownNow();
        }
    }

    private static ToolCallback recordingTool(String name, String marker, java.util.List<String> order) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                        .name(name).description("记录完成序的工具").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                order.add(marker);
                return "ok:" + toolInput;
            }
        };
    }

    @Test
    void shouldTimeoutWithStructuredErrorWhenLaneFull() throws Exception {
        PriorityLane lane = new PriorityLane(1);
        lane.acquire(0, Duration.ofSeconds(1)); // 占满

        PriorityLaneToolCallback wrapped = new PriorityLaneToolCallback(
                tool("slow_query"), lane, 3, Duration.ofMillis(100));
        assertThatThrownBy(() -> wrapped.call("q"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("工具泳道许可等待超时");

        lane.release(); // 归还后同回调可用
        assertThat(wrapped.call("q2")).isEqualTo("ok:q2");
    }

    @Test
    void shouldReleasePermitOnFailure() {
        PriorityLane lane = new PriorityLane(1);
        ToolCallback failing = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return DefaultToolDefinition.builder()
                        .name("boom").description("会炸的工具").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException("工具本身炸了");
            }
        };
        PriorityLaneToolCallback wrapped = new PriorityLaneToolCallback(
                failing, lane, 3, Duration.ofSeconds(1));
        assertThatThrownBy(() -> wrapped.call("q"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("工具本身炸了");
        // 异常路径已归还——立即取许可成功（无需 release）
        assertThat(lane.tryAcquire(3)).isTrue();
    }

    @Test
    void shouldReturnNamedSingletonPriorityLanes() {
        ToolLaneRegistry registry = new ToolLaneRegistry();
        PriorityLane first = registry.priorityLane("slow-db", 2);
        PriorityLane again = registry.priorityLane("slow-db", 99); // 同名忽略新 permits
        PriorityLane other = registry.priorityLane("fast-cache", 8);
        assertThat(first).isSameAs(again);
        assertThat(first).isNotSameAs(other);
        assertThat(first.permits()).isEqualTo(2);
        // 与 Semaphore 泳道分仓：同名互不串扰
        assertThat(registry.lane("slow-db", 4)).isNotNull();
        assertThat(registry.priorityLane("slow-db", 2)).isSameAs(first);
    }

    @Test
    void shouldAssembleFromYml_andFailFastOnUnknownLane() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.tool-lanes.lanes.slow-db.permits=2",
                        "buzhou.tool-lanes.lanes.slow-db.acquire-timeout=5s",
                        "buzhou.tool-lanes.tools.query_customer.lane=slow-db",
                        "buzhou.tool-lanes.tools.query_customer.priority=3")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouToolLaneRuntimeConfig");
                });
        // 未配置零装配
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouToolLaneRuntimeConfig");
                });
        // tools 引用未声明泳道：启动即红（fail-fast）
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.tool-lanes.lanes.slow-db.permits=2",
                        "buzhou.tool-lanes.tools.query_customer.lane=missing-lane")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldWrapThroughRuntime_endToEnd() {
        RuntimeConfig config = new RuntimeConfig(List.of(), java.util.Set.of(), java.util.Set.of(),
                null, List.of(), Map.of(), List.of(),
                List.of(ctx -> ctx.wrapToolCallbacks(cb -> "slow_query".equals(cb.getToolDefinition().name())
                        ? new PriorityLaneToolCallback(cb, new PriorityLane(2), 3, Duration.ofSeconds(5))
                        : cb)),
                null);
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("ok");
        // E2E 装配验证：runtime 能用该 config 起（包装 customizer 不炸）
        try (var agent = Buzhou.runtime(model, Buzhou.inMemoryStores(), config)
                .spawn("app", "ag", "s-lane")) {
            assertThat(agent.chat("hi")).isNotNull();
        }
    }
}
