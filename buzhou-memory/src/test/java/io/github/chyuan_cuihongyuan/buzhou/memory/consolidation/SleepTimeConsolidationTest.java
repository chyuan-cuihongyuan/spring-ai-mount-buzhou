package io.github.chyuan_cuihongyuan.buzhou.memory.consolidation;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-11 / T37 sleep-time 集成：每 N Turn 异步触发整理（热路径零阻塞）、
 * 每 session 串行、无摘要 NOOP 韧性、整理动作走对账（失败不外溢）。
 */
class SleepTimeConsolidationTest {

    private static final String NINE_SECTIONS = """
            ## USER_INTENT
            排查订单
            ## CURRENT_STATE
            第 3 步
            ## NEXT_STEP
            查网关
            ## PENDING_TASKS
            无
            ## ERRORS_FIXES
            无
            ## KEY_ARTIFACTS
            PAY-1
            ## PROBLEM_SOLVING
            定位
            ## TECHNICAL_CONCEPTS
            状态机
            ## USER_MESSAGES_LOG
            若干
            """;

    @Test
    void everyNTurnsTriggersAsyncConsolidationOffHotPath() throws Exception {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "sleep-" + UUID.randomUUID();
        // 预置一份摘要（整理器有事可做：对最新摘要重跑对账）
        SummaryStoreBridge bridge = new SummaryStoreBridge(stores.summaryStore());
        bridge.save(sessionId, NineSectionSummary.empty());

        CountDownLatch consolidated = new CountDownLatch(1);
        AtomicLong chatReturnedAt = new AtomicLong();
        AtomicLong consolidatedAt = new AtomicLong();
        // 主模型回复前阻塞 300ms：若整理在热路径同步执行，chat 必然也被拖慢（对照组）
        ChatModel main = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
            }
        };
        SleepTimeConsolidator consolidator = new SleepTimeConsolidator(bridge,
                summaryModel(), stores.sessionStateStore(),
                outcome -> {
                    consolidatedAt.set(System.nanoTime());
                    consolidated.countDown();
                });
        SleepTimeScheduler scheduler = new SleepTimeScheduler();
        SleepTimeConsolidationHook hook = new SleepTimeConsolidationHook(scheduler, consolidator, 2);
        var config = new io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig(
                List.of(hook), java.util.Set.of(), java.util.Set.of(), null, List.of());

        AgentRuntime runtime = Buzhou.runtime(main, stores, config);
        AgentSession session = runtime.spawn("sleep-app", "agent", sessionId);
        session.chat("第 1 轮"); // turn 1：不触发（every=2）
        session.chat("第 2 轮"); // turn 2：触发异步整理
        chatReturnedAt.set(System.nanoTime());
        session.close();

        // 整理确实发生（异步等待），且在 chat 返回之后完成（热路径零阻塞证据）
        assertThat(consolidated.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(consolidatedAt.get()).isGreaterThan(chatReturnedAt.get());
        scheduler.close();
    }

    @Test
    void perSessionTasksAreSerialized() throws Exception {
        List<String> order = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(3);
        SleepTimeScheduler scheduler = new SleepTimeScheduler();
        for (int i = 1; i <= 3; i++) {
            int seq = i;
            scheduler.submit("same-session", () -> {
                try {
                    Thread.sleep(30); // 若非串行，后提交者可能先完成
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                order.add("task-" + seq);
                done.countDown();
            });
        }
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(order).containsExactly("task-1", "task-2", "task-3");
        scheduler.close();
    }

    @Test
    void noSummaryConsolidatesAsNoop() throws Exception {
        BuzhouStores stores = Buzhou.inMemoryStores();
        CountDownLatch latch = new CountDownLatch(1);
        List<String> details = new CopyOnWriteArrayList<>();
        SleepTimeConsolidator consolidator = new SleepTimeConsolidator(
                new SummaryStoreBridge(stores.summaryStore()), summaryModel(), null,
                outcome -> {
                    details.add(outcome.detail());
                    latch.countDown();
                });
        consolidator.consolidate("no-summary-session");
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(details).containsExactly("no-summary");
    }

    private static ChatModel summaryModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage(NINE_SECTIONS))));
            }
        };
    }
}
