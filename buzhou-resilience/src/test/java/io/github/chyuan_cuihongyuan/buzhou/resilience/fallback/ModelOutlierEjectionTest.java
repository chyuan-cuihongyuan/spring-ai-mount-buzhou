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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 149 / T506：离群驱逐回归——连错逐出 / 窗口复池 / success 复位 /
 * filter 保序剔除 / 互不影响 / 参数校验。
 */
class ModelOutlierEjectionTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.now();

        void advanceMillis(long ms) {
            now = now.plusMillis(ms);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    /** 快测配置：3 连错 / 窗口 200ms。 */
    private static ModelOutlierEjection.Config fast() {
        return new ModelOutlierEjection.Config(3, Duration.ofMillis(200));
    }

    private static NamedFallbackModel model(String name) {
        ChatModel stub = new ChatModel() {
            @Override
            public org.springframework.ai.chat.model.ChatResponse call(Prompt prompt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Flux<org.springframework.ai.chat.model.ChatResponse> stream(Prompt prompt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
                return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
            }
        };
        return new NamedFallbackModel(name, stub);
    }

    @Test
    void consecutiveErrorsEjectAndWindowReadmits() {
        MutableClock clock = new MutableClock();
        ModelOutlierEjection ejection = new ModelOutlierEjection(fast(), clock);

        ejection.recordError("bad");
        ejection.recordError("bad");
        assertThat(ejection.isEjected("bad")).isFalse();
        ejection.recordError("bad"); // 3 连错 → 逐出
        assertThat(ejection.isEjected("bad")).isTrue();

        clock.advanceMillis(250); // 窗口过 → 复池
        assertThat(ejection.isEjected("bad")).isFalse();
    }

    @Test
    void successResetsConsecutiveCount() {
        ModelOutlierEjection ejection = new ModelOutlierEjection(fast(), new MutableClock());
        ejection.recordError("flaky");
        ejection.recordError("flaky");
        ejection.recordSuccess("flaky"); // 复位
        ejection.recordError("flaky");   // 只算第 1
        assertThat(ejection.isEjected("flaky")).isFalse();
    }

    @Test
    void filterRemovesEjectedPreservingOrder() {
        MutableClock clock = new MutableClock();
        ModelOutlierEjection ejection = new ModelOutlierEjection(fast(), clock);
        for (int i = 0; i < 3; i++) {
            ejection.recordError("m2");
        }

        List<NamedFallbackModel> healthy = ejection.filter(List.of(
                model("m1"), model("m2"), model("m3")));

        assertThat(healthy).extracting(NamedFallbackModel::name)
                .containsExactly("m1", "m3");
        assertThat(ejection.ejectedModels()).containsExactly("m2");
    }

    @Test
    void modelsAreIsolated() {
        ModelOutlierEjection ejection = new ModelOutlierEjection(fast(), new MutableClock());
        for (int i = 0; i < 3; i++) {
            ejection.recordError("a");
        }
        assertThat(ejection.isEjected("b")).isFalse();
        assertThat(ejection.ejectedModels()).containsExactly("a");
    }

    @Test
    void ejectionRecountsAfterReadmission() {
        MutableClock clock = new MutableClock();
        ModelOutlierEjection ejection = new ModelOutlierEjection(fast(), clock);
        for (int i = 0; i < 3; i++) {
            ejection.recordError("bad");
        }
        clock.advanceMillis(250); // 复池
        assertThat(ejection.isEjected("bad")).isFalse();

        ejection.recordError("bad"); // 复池后重新计数
        ejection.recordError("bad");
        assertThat(ejection.isEjected("bad")).isFalse(); // 未达 3
        ejection.recordError("bad");
        assertThat(ejection.isEjected("bad")).isTrue(); // 再逐
    }

    @Test
    void configValidatedFailFast() {
        assertThatThrownBy(() -> new ModelOutlierEjection.Config(0, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ModelOutlierEjection.Config(3, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
