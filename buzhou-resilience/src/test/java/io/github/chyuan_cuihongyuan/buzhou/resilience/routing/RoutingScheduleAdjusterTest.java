package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 503 / T757–T758：时段路由窗口——Clock 注入窗内应用窗权/窗外回落
 * 基础/同快照幂等零动作/未知 bean 名跳过/生命周期/yml 装配缺席。
 */
class RoutingScheduleAdjusterTest {

    private static final class DummyChatModel implements ChatModel {
        @Override
        public org.springframework.ai.chat.model.ChatResponse call(
                org.springframework.ai.chat.prompt.Prompt prompt) {
            throw new UnsupportedOperationException("时段路由测试不发起真实调用");
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return null;
        }
    }

    private static WeightedChatModel chatModel() {
        return new WeightedChatModel(
                Map.of("cheap", new DummyChatModel(), "strong", new DummyChatModel()),
                Map.of("cheap", 1, "strong", 9));
    }

    private static Clock clockAt(String hhmm) {
        return Clock.fixed(LocalTime.parse(hhmm).atDate(java.time.LocalDate.of(2026, 9, 12))
                .atZone(ZoneId.of("Asia/Shanghai")).toInstant(), ZoneId.of("Asia/Shanghai"));
    }

    private static BuzhouRoutingScheduleProperties.RoutingWindow window(String start, String end,
            Map<String, Integer> weights) {
        return new BuzhouRoutingScheduleProperties.RoutingWindow(
                LocalTime.parse(start), LocalTime.parse(end), weights);
    }

    private static final Map<String, Integer> BASE = Map.of("cheap", 1, "strong", 9);

    @Test
    void insideWindowAppliesWindowWeights() {
        WeightedChatModel model = chatModel();
        RoutingScheduleAdjuster adjuster = new RoutingScheduleAdjuster(model,
                List.of(window("08:00", "20:00", Map.of("cheap", 9, "strong", 1))),
                BASE, Duration.ofSeconds(30), clockAt("12:00"));
        adjuster.evaluateOnce(Instant.now(clockAt("12:00")));
        assertThat(model.routes()).containsEntry("cheap", 9).containsEntry("strong", 1);
    }

    @Test
    void outsideWindowFallsBackToBaseWeights() {
        WeightedChatModel model = chatModel();
        RoutingScheduleAdjuster adjuster = new RoutingScheduleAdjuster(model,
                List.of(window("08:00", "20:00", Map.of("cheap", 9, "strong", 1))),
                BASE, Duration.ofSeconds(30), clockAt("12:00"));
        adjuster.evaluateOnce(Instant.now(clockAt("12:00")));
        assertThat(model.routes()).containsEntry("cheap", 9);
        // 出窗回落基础
        adjuster.evaluateOnce(Instant.now(clockAt("23:00")));
        assertThat(model.routes()).containsEntry("cheap", 1).containsEntry("strong", 9);
    }

    @Test
    void sameSnapshotIsIdempotentNoOp() {
        WeightedChatModel model = chatModel();
        RoutingScheduleAdjuster adjuster = new RoutingScheduleAdjuster(model,
                List.of(window("08:00", "20:00", Map.of("cheap", 9, "strong", 1))),
                BASE, Duration.ofSeconds(30), clockAt("12:00"));
        adjuster.evaluateOnce(Instant.now(clockAt("12:00")));
        String snapshot = adjuster.appliedSnapshot();
        // 同窗重复 tick：快照未变 → 零动作（不重复 setWeight——幂等）
        adjuster.evaluateOnce(Instant.now(clockAt("12:00")));
        assertThat(adjuster.appliedSnapshot()).isEqualTo(snapshot);
    }

    @Test
    void firstMatchingWindowWinsAndUnknownBeanSkipped() {
        WeightedChatModel model = chatModel();
        RoutingScheduleAdjuster adjuster = new RoutingScheduleAdjuster(model,
                List.of(window("00:00", "12:00", Map.of("cheap", 5, "strong", 5)),
                        window("06:00", "23:00", Map.of("cheap", 9, "ghost", 3, "strong", 1))),
                BASE, Duration.ofSeconds(30), clockAt("12:00"));
        // 11:00 两窗都含（end 排他）→ 序优先取首窗
        adjuster.evaluateOnce(Instant.now(clockAt("11:00")));
        assertThat(model.routes()).containsEntry("cheap", 5).containsEntry("strong", 5);
        // 18:00 只命中第二窗：未知 ghost 跳过不炸
        adjuster.evaluateOnce(Instant.now(clockAt("18:00")));
        assertThat(model.routes()).containsEntry("cheap", 9).containsEntry("strong", 1);
    }

    @Test
    void lifecycleStartStopTransitions() throws Exception {
        WeightedChatModel model = chatModel();
        RoutingScheduleAdjuster adjuster = new RoutingScheduleAdjuster(model,
                List.of(window("00:00", "23:59", Map.of("cheap", 9, "strong", 1))),
                BASE, Duration.ofSeconds(30), Clock.systemUTC());
        assertThat(adjuster.isRunning()).isFalse();
        adjuster.start();
        assertThat(adjuster.isRunning()).isTrue();
        adjuster.stop();
        assertThat(adjuster.isRunning()).isFalse();
    }

    @Test
    void ymlAssemblyOnlyWhenWindowsDeclared() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.resilience.config
                                .BuzhouResilienceAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.routing.weights.cheap=5",
                        "buzhou.routing.weights.strong=5",
                        "buzhou.routing.schedule.windows[0].start=08:00",
                        "buzhou.routing.schedule.windows[0].end=20:00",
                        "buzhou.routing.schedule.windows[0].weights.cheap=9",
                        "buzhou.routing.schedule.windows[0].weights.strong=1")
                .withBean("cheap", ChatModel.class, DummyChatModel::new)
                .withBean("strong", ChatModel.class, DummyChatModel::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("routingScheduleAdjuster");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.resilience.config
                                .BuzhouResilienceAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.routing.weights.cheap=5",
                        "buzhou.routing.weights.strong=5")
                .withBean("cheap", ChatModel.class, DummyChatModel::new)
                .withBean("strong", ChatModel.class, DummyChatModel::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("routingScheduleAdjuster");
                });
    }
}
