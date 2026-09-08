package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 408 §Testing / T707–T708：日历周期预算——双轨累计；翻页隐式重置
 * （Clock 换周期）；tokens/cost 闸各自治闸；软预警一次一发；fail-fast 与
 * yml 装配。
 */
class PeriodBudgetHookTest {

    private static final class SettableClock extends Clock {
        volatile Instant now = Instant.parse("2026-09-08T00:00:00Z");

        void plusDays(long d) {
            now = now.plusSeconds(d * 86_400);
        }

        @Override public Instant instant() { return now; }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    private static final class Call implements ModelCallContext {
        final HookEnvironment env;
        final ChatClientResponse response;
        final List<SessionEvent> events;

        Call(HookEnvironment env, ChatClientResponse response, List<SessionEvent> events) {
            this.env = env;
            this.response = response;
            this.events = events;
        }

        @Override public String sessionId() { return env.sessionId(); }
        @Override public String agentName() { return env.agentName(); }
        @Override public int turn() { return 1; }
        @Override public SessionStateHandle state() { return env.stateHandle(); }
        @Override public void emitEvent(SessionEvent event) { events.add(event); }
        @Override public ChatClientRequest request() { return null; }
        @Override public ChatClientResponse response() { return response; }
        @Override public Throwable error() { return null; }
        @Override public void replaceRequest(ChatClientRequest r) { }
        @Override public void replaceResponse(ChatClientResponse r) { }
    }

    private static ChatClientResponse responseWith(long promptTokens, long completionTokens) {
        Usage usage = new org.springframework.ai.chat.metadata.DefaultUsage(
                (int) promptTokens, (int) completionTokens);
        ChatResponse chat = new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))),
                org.springframework.ai.chat.metadata.ChatResponseMetadata.builder()
                        .usage(usage).build());
        return new ChatClientResponse(chat, Map.of());
    }

    @Test
    void shouldAccumulateDualTracks_andResetOnPeriodRollover() {
        SettableClock clock = new SettableClock();
        SessionStateStore store = new InMemorySessionStateStore();
        PeriodBudgetHook hook = new PeriodBudgetHook(store, PeriodBudgetHook.Unit.MONTHLY,
                null, null, 100, Map.of("m", new PeriodBudgetHook.Pricing(
                        new BigDecimal("1.000"), new BigDecimal("2.000"))), "m", clock);
        assertThat(hook.periodTag()).isEqualTo("2026-09");

        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        hook.afterModel(new Call(new HookEnvironment("s", "a", store),
                responseWith(100, 50), events));  // 150 tokens；cost = 100*1 + 50*2 = 200 micro
        hook.afterModel(new Call(new HookEnvironment("s", "a", store),
                responseWith(10, 5), events));
        assertThat(hook.periodTokens()).isEqualTo(165L);
        assertThat(hook.periodCostMicroUsd()).isEqualTo(220L);
        assertThat(events).isEmpty(); // 无限额 → 无预警维度

        // 翻页：换到 10 月（MONTHLY tag 变）→ 累计隐式归零
        clock.plusDays(31);
        assertThat(hook.periodTag()).isEqualTo("2026-10");
        assertThat(hook.periodTokens()).isZero();
        assertThat(hook.periodCostMicroUsd()).isZero();
    }

    @Test
    void shouldGateOnTokensLimit_atNextBeforeModel() {
        SettableClock clock = new SettableClock();
        SessionStateStore store = new InMemorySessionStateStore();
        PeriodBudgetHook hook = new PeriodBudgetHook(store, PeriodBudgetHook.Unit.DAILY,
                100L, null, 80, Map.of(), "m", clock);
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        HookEnvironment env = new HookEnvironment("s", "a", store);

        assertThat(hook.beforeModel(new Call(env, null, events)))
                .isInstanceOf(HookResult.Continue.class);
        hook.afterModel(new Call(env, responseWith(60, 40), events));   // 100/100 → 预警
        assertThat(events).extracting(SessionEvent::type)
                .contains(PeriodBudgetHook.EVENT_WARNING);
        HookResult blocked = hook.beforeModel(new Call(env, null, events));
        assertThat(blocked).isInstanceOf(HookResult.Block.class);
        assertThat(events).extracting(SessionEvent::type)
                .contains(PeriodBudgetHook.EVENT_EXCEEDED);

        // 翻页解封：次日（DAILY tag 变）
        clock.plusDays(1);
        assertThat(hook.beforeModel(new Call(env, null, events)))
                .isInstanceOf(HookResult.Continue.class);
    }

    @Test
    void shouldGateOnCostTrack_whenPricingConfigured() {
        SessionStateStore store = new InMemorySessionStateStore();
        // 价 1 USD/M in、2 USD/M out：60 in + 20 out = 60*1 + 20*2 = 100 micro
        PeriodBudgetHook hook = new PeriodBudgetHook(store, PeriodBudgetHook.Unit.MONTHLY,
                null, 100L, 100, Map.of("m", new PeriodBudgetHook.Pricing(
                        new BigDecimal("1.000"), new BigDecimal("2.000"))), "m",
                new SettableClock());
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        HookEnvironment env = new HookEnvironment("s", "a", store);
        hook.afterModel(new Call(env, responseWith(60, 20), events));
        HookResult blocked = hook.beforeModel(new Call(env, null, events));
        assertThat(blocked).isInstanceOf(HookResult.Block.class)
                .asString().contains("成本预算");
    }

    @Test
    void shouldAssembleFromYml_withFailFastOnMissingLimits() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.budget.period.enabled=true",
                        "buzhou.budget.period.unit=weekly",
                        "buzhou.budget.period.tokens-limit=5000000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouPeriodBudgetRuntimeConfig");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues("buzhou.budget.period.enabled=true")
                .run(context -> assertThat(context).hasFailed());
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouPeriodBudgetRuntimeConfig");
                });
    }
}
