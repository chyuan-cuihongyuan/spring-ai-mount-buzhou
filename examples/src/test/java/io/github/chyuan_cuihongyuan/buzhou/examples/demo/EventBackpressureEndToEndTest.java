package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.EventBusStats;
import io.github.chyuan_cuihongyuan.buzhou.core.session.EventDispatchConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.FakeModelGuard;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-34 / spec 13 §core-4（事件背压与线程卫生）端到端（FakeChatModel 驱动）：
 *
 * <ul>
 *   <li><b>慢监听不拖慢 Turn</b>：buffered 模式下事件异步交付，Turn 主链路毫秒级返回
 *       （同步模式会等监听器 sleep 完才返回）；</li>
 *   <li><b>坏监听被隔离</b>：抛异常的监听器不影响其余监听器继续收到事件、Turn 正常完成；</li>
 *   <li><b>丢弃可见</b>：容量打满的丢弃经 {@link AgentSession#eventBusStats()} 可断言。</li>
 * </ul>
 */
class EventBackpressureEndToEndTest {

    /** 每轮 beforeTurn 发 6 个事件（容量 2 必然溢出 → 丢弃可断言）。 */
    private static final class EventfulHook implements BuzhouHook {
        @Override
        public HookResult beforeTurn(TurnContext ctx) {
            for (int i = 0; i < 6; i++) {
                ctx.emitEvent(SessionEvent.of("hook.tick." + i));
            }
            return HookResult.CONTINUE;
        }
    }

    @Test
    void bufferedModeSlowListenerDoesNotSlowTurnAndBadListenerIsIsolated() throws Exception {
        FakeChatModel model = FakeChatModel.script(ScriptStep.text("done"));
        FakeModelGuard.requireTestDouble(model);

        List<String> goodReceived = new CopyOnWriteArrayList<>();
        BuzhouStores stores = Buzhou.inMemoryStores();

        // buffered + 容量 2：慢监听占住交付线程，6 个事件只留最新、其余丢弃（计数可见）
        EventDispatchConfig dispatch = new EventDispatchConfig(
                EventDispatchConfig.Mode.BUFFERED, 2,
                EventDispatchConfig.OverflowPolicy.DROP_OLDEST, null);

        AgentRuntime runtime = Buzhou.runtime(model, stores,
                RuntimeConfig.hooks(List.of(new EventfulHook())),
                null, null, null, dispatch);
        AgentSession session = runtime.spawn("e2e-app", "agent", "backpressure-e2e");
        session.addEventListener(event -> sleepQuietly(1_500)); // 慢监听：占住交付线程
        session.addEventListener(event -> {
            throw new IllegalStateException("坏监听器：异常必须被隔离");
        });
        session.addEventListener(event -> goodReceived.add(event.type()));

        long start = System.nanoTime();
        String reply = session.chat("hello");
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        // 慢监听（1.5s）不拖慢 Turn：chat 毫秒级返回（同步模式将 ≥ 1.5s）
        assertThat(reply).isEqualTo("done");
        assertThat(elapsedMillis).isLessThan(1_200);

        // 坏监听被隔离：好监听器照常收到事件（异步交付，轮询上限 3s）
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
        while (goodReceived.isEmpty() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertThat(goodReceived).isNotEmpty();

        // 丢弃可见：6 事件 × 容量 2 → dropped ≥ 1（等待异步丢弃计数落地）
        deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
        EventBusStats stats = session.eventBusStats().orElseThrow();
        while (stats.dropped() < 1 && System.nanoTime() < deadline) {
            Thread.sleep(20);
            stats = session.eventBusStats().orElseThrow();
        }
        assertThat(stats.dropped()).isGreaterThanOrEqualTo(1);
        session.close();
    }

    @Test
    void syncModeDeliversInlineAndStatsEmpty() {
        FakeChatModel model = FakeChatModel.script(ScriptStep.text("ok"));
        FakeModelGuard.requireTestDouble(model);

        List<String> received = new CopyOnWriteArrayList<>();
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                RuntimeConfig.hooks(List.of(new EventfulHook())));
        AgentSession session = runtime.spawn("e2e-app", "agent", "sync-e2e");
        session.addEventListener(event -> received.add(event.type()));

        assertThat(session.chat("hello")).isEqualTo("ok");
        // 同步内联交付：chat 返回前监听器已收到全部事件；SYNC 无队列语义（stats 为空）
        assertThat(received).hasSize(6);
        assertThat(session.eventBusStats()).isEmpty();
        session.close();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
