package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 195 / T568：降级链演练回归——成功记新 / 失败保留旧证 / 过期剔除保序 /
 * 异常吞 / 未注册 / 隔离。
 */
class FallbackDrillTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.now();

        void advanceSeconds(long s) {
            now = now.plusSeconds(s);
        }

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private static NamedFallbackModel model(String name) {
        ChatModel stub = new ChatModel() {
            @Override public org.springframework.ai.chat.model.ChatResponse call(Prompt p) {
                throw new UnsupportedOperationException();
            }
            @Override public Flux<org.springframework.ai.chat.model.ChatResponse> stream(Prompt p) {
                throw new UnsupportedOperationException();
            }
            @Override public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
                return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
            }
        };
        return new NamedFallbackModel(name, stub);
    }

    @Test
    void successfulDrillMarksFreshAndFilterKeeps() {
        MutableClock clock = new MutableClock();
        FallbackDrill drill = new FallbackDrill(clock);
        drill.register("backup-1", () -> true);
        drill.register("backup-2", () -> true);

        FallbackDrill.DrillOutcome outcome = drill.drillAll();
        assertThat(outcome.ok()).containsExactlyInAnyOrder("backup-1", "backup-2");
        assertThat(outcome.failed()).isEmpty();

        assertThat(drill.isFresh("backup-1", Duration.ofMinutes(10))).isTrue();
        assertThat(drill.filter(List.of(model("backup-1"), model("backup-2")),
                Duration.ofMinutes(10))).hasSize(2);

        clock.advanceSeconds(601); // 超过 maxAge
        assertThat(drill.isFresh("backup-1", Duration.ofMinutes(10))).isFalse();
        assertThat(drill.filter(List.of(model("backup-1")), Duration.ofMinutes(10)))
                .isEmpty();
    }

    @Test
    void failedDrillKeepsPriorVerificationFact() {
        MutableClock clock = new MutableClock();
        FallbackDrill drill = new FallbackDrill(clock);
        AtomicBoolean healthy = new AtomicBoolean(true);
        drill.register("flaky", healthy::get);

        assertThat(drill.drill("flaky")).isTrue();
        healthy.set(false);
        assertThat(drill.drill("flaky")).isFalse();

        FallbackDrill.DrillState state = drill.states().get("flaky");
        assertThat(state.lastVerifiedAt()).isNotNull(); // 旧证保留
        assertThat(state.lastFailedAt()).isNotNull();
        assertThat(drill.isFresh("flaky", Duration.ofMinutes(10))).isTrue(); // 验证事实仍在窗内
    }

    @Test
    void probeExceptionIsSwallowedAsFailure() {
        FallbackDrill drill = new FallbackDrill(new MutableClock());
        drill.register("boom", () -> {
            throw new IllegalStateException("credentials expired");
        });

        assertThat(drill.drill("boom")).isFalse(); // 异常吞——演练不扰生产
        assertThat(drill.states().get("boom").lastFailedAt()).isNotNull();
    }

    @Test
    void unregisteredModelIsNeitherFreshNorDrillable() {
        FallbackDrill drill = new FallbackDrill(new MutableClock());
        assertThat(drill.drill("ghost")).isFalse();
        assertThat(drill.isFresh("ghost", Duration.ofHours(1))).isFalse(); // 从未验证不算新鲜
    }

    @Test
    void filterPreservesOrderAndDropsStaleOnly() {
        MutableClock clock = new MutableClock();
        FallbackDrill drill = new FallbackDrill(clock);
        drill.register("fresh-1", () -> true);
        drill.register("stale", () -> true);
        drill.drillAll();

        clock.advanceSeconds(700); // 全部过 10min 窗
        drill.register("fresh-2", () -> true);
        drill.drill("fresh-2"); // 只有 fresh-2 有新证

        List<NamedFallbackModel> fresh = drill.filter(
                List.of(model("fresh-1"), model("stale"), model("fresh-2")),
                Duration.ofMinutes(10));
        assertThat(fresh).extracting(NamedFallbackModel::name)
                .containsExactly("fresh-2"); // 保序 + 只留新证
    }

    @Test
    void registerValidatesArguments() {
        assertThatThrownBy(() -> new FallbackDrill().register(" ", () -> true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FallbackDrill().register("m", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
