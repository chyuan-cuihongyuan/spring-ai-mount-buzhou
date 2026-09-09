package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.budget.PeriodBudgetHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 419 §Testing / T729–T730：周期预算健康面——双轨进度+pct 截断+
 * exhausted+resetsAt 三粒度；未启用 UNKNOWN；装配同键。
 */
class PeriodBudgetHealthTest {

    private static final class FixedClock extends Clock {
        private final Instant now;

        FixedClock(String at) {
            this.now = Instant.parse(at);
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

    private static ChatClientResponse usage(long prompt, long completion) {
        ChatResponse chat = new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))),
                ChatResponseMetadata.builder()
                        .usage(new DefaultUsage((int) prompt, (int) completion)).build());
        return new ChatClientResponse(chat, Map.of());
    }

    @Test
    void shouldReportDualTrackProgressWithResetsAt() {
        SessionStateStore store = new InMemorySessionStateStore();
        FixedClock clock = new FixedClock("2026-09-08T10:15:00Z");
        PeriodBudgetHook hook = new PeriodBudgetHook(store, PeriodBudgetHook.Unit.MONTHLY,
                100L, null, 80, Map.of(), "m", clock);
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        hook.afterModel(new Call(new HookEnvironment("s", "a", store), usage(60, 40), events));

        PeriodBudgetHealth health = new PeriodBudgetHealth(hook, PeriodBudgetHook.Unit.MONTHLY,
                100L, null, clock);
        assertThat(health.mechanism()).isEqualTo("period-budget");
        assertThat(health.status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(health.details())
                .containsEntry("tokens", 100L)
                .containsEntry("tokensLimit", 100L)
                .containsEntry("tokensPct", 100)
                .containsEntry("exhaustedTokens", true)
                .containsEntry("period", "2026-09")
                .containsEntry("resetsAt", "2026-10-01T00:00:00Z")
                .containsKey("costMicroUsd")
                .doesNotContainKey("costPct"); // 成本轨无限额不出现

        assertThat(new PeriodBudgetHealth(null, null, null, null, null).status())
                .isEqualTo(BuzhouHealth.Status.UNKNOWN);
    }

    @Test
    void shouldComputeResetsAtForWeeklyAndDaily() {
        FixedClock clock = new FixedClock("2026-09-08T10:15:00Z");
        PeriodBudgetHealth weekly = new PeriodBudgetHealth(null, PeriodBudgetHook.Unit.WEEKLY,
                null, null, clock);
        // epoch 周界锚 1970-01-01（周四）——非自然周一；09-08 的下周界 = 09-10
        assertThat(weekly.resetsAt()).isEqualTo(Instant.parse("2026-09-10T00:00:00Z"));

        PeriodBudgetHealth daily = new PeriodBudgetHealth(null, PeriodBudgetHook.Unit.DAILY,
                null, null, clock);
        assertThat(daily.resetsAt()).isEqualTo(Instant.parse("2026-09-09T00:00:00Z"));
    }

    @Test
    void shouldAssembleWithSameKeyAsBudget() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.budget.period.enabled=true",
                        "buzhou.budget.period.tokens-limit=1000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouPeriodBudgetHealth");
                    assertThat(context).hasBean("buzhouPeriodBudgetRuntimeConfig");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean("buzhouPeriodBudgetHealth");
                    assertThat(context).doesNotHaveBean("buzhouPeriodBudgetRuntimeConfig");
                });
    }
}
