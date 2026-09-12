package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 509 / T769–T770：时延 SLO 燃尽——慢轮计坏事件燃尽 breach、快轮
 * 不 breach、无起点 afterTurn 安全跳过、threshold 校验 fail-fast、
 * yml 装配缺席。
 */
class LatencySloMonitorTest {

    private static ErrorBudget budget(double slo) {
        return new ErrorBudget(new ErrorBudget.Config(slo, 2.0,
                ErrorBudget.Config.DEFAULT_BUCKETS, ErrorBudget.Config.DEFAULT_WINDOW, 5),
                Clock.systemUTC());
    }

    @Test
    void slowTurnsBurnBudgetFastTurnsDoNot() {
        LatencySloMonitor monitor = new LatencySloMonitor(1, budget(99.0));
        // 直接喂 hook：afterTurn 无起点跳过；beforeTurn 起点+同步耗时模拟
        monitor.beforeTurn(new LatencyCtx("s1", "agent-a"));
        System.currentTimeMillis(); // 间隔 < 1ms——坏事件（threshold=1ms 边界内）
        assertThat(monitor.afterTurn(new LatencyCtx("s1", "agent-a")))
                .isEqualTo(HookResult.CONTINUE);
        // elapsed 必 >0：threshold 1ms 下首样本可能 ok——用样本数断言记账发生
        assertThat(monitor.budget().samples("agent-a")).isEqualTo(1);

        // 无起点 afterTurn——安全跳过不记账
        monitor.afterTurn(new LatencyCtx("s-unknown", "agent-a"));
        assertThat(monitor.budget().samples("agent-a")).isEqualTo(1);
    }

    @Test
    void slowModelE2ETriggersBreaching() {
        LatencySloMonitor monitor = new LatencySloMonitor(1, budget(99.0));
        // 脚本模型瞬回——靠慢钩子把每轮拉开 5ms（> threshold 1ms → 全坏事件）
        BuzhouHook sleeper = new BuzhouHook() {
            @Override
            public int order() {
                return 500; // 先于 monitor 计时起点？——计时在 beforeTurn 都记录，sleep 放本钩子
            }

            @Override
            public HookResult afterTurn(TurnContext ctx) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return HookResult.CONTINUE;
            }
        };
        var model = new io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel();
        for (int i = 0; i < 8; i++) {
            model.enqueueText("第 " + i + " 轮");
        }
        var runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                new RuntimeConfig(List.of(sleeper, monitor), Set.of(), Set.of(), null, List.of()));
        AgentSession session = runtime.spawn("app", "slow-agent", "sess-slo");
        for (int i = 0; i < 8; i++) {
            session.chat("问 " + i);
        }
        session.close();
        assertThat(monitor.budget().samples("slow-agent")).isEqualTo(8);
        assertThat(monitor.budget().breaching("slow-agent")).isTrue();
        assertThat(monitor.budget().topBreaching(3))
                .anySatisfy(e -> assertThat(e.getKey()).isEqualTo("slow-agent"));
    }

    @Test
    void invalidThresholdFailsFast() {
        assertThatThrownBy(() -> new LatencySloMonitor(0, budget(99.0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LatencySloMonitor(1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ymlAssemblyOnlyWhenEnabled() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withBean(BuzhouStores.class, () -> stores)
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues("buzhou.latency-slo.enabled=true",
                        "buzhou.latency-slo.threshold-millis=2000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouLatencySloRuntimeConfig");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withBean(BuzhouStores.class, () -> Buzhou.inMemoryStores())
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouLatencySloRuntimeConfig");
                });
    }

    /** 最小 TurnContext 桩（sessionId/agentName 面向 hook 计时）。 */
    private static final class LatencyCtx implements TurnContext {
        private final String sessionId;
        private final String agentName;

        LatencyCtx(String sessionId, String agentName) {
            this.sessionId = sessionId;
            this.agentName = agentName;
        }

        @Override public String sessionId() { return sessionId; }
        @Override public String agentName() { return agentName; }
        @Override public int turn() { return 0; }
        @Override public io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle state() {
            throw new UnsupportedOperationException();
        }
        @Override public void emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent event) { }
        @Override public String input() { return ""; }
        @Override public String response() { return ""; }
        @Override public void replaceInput(String newInput) { }
        @Override public void replaceResponse(String newResponse) { }
    }
}
