package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 709 / T1018–T1019：实验到期自动停——未到期正常分桶、过期返回 null
 * +__expired__ 独立桶、无到期声明零影响、构造 fail-fast、读数往返。
 */
class ExperimentExpiryTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-12T00:00:00Z");

        void advanceSeconds(long s) {
            now = now.plusSeconds(s);
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

    @Test
    void expiredExperimentStopsAssigningAndCountsSeparately() {
        MutableClock clock = new MutableClock();
        ExperimentBucketer bucketer = new ExperimentBucketer(
                Map.of("exp-a", Map.of("treatment", 100)),
                Map.of("exp-a", Instant.parse("2026-09-12T00:01:00Z")),
                clock);

        assertThat(bucketer.assign("exp-a", "u1")).isEqualTo("treatment"); // 未到期正常入组
        assertThat(bucketer.expiredExperiments()).isEmpty();

        clock.advanceSeconds(61); // 过期 1 秒
        assertThat(bucketer.assign("exp-a", "u2")).isNull();
        assertThat(bucketer.assign("exp-a", "u2")).isNull(); // 幂等
        assertThat(bucketer.expiredExperiments()).containsExactly("exp-a");
        assertThat(bucketer.snapshot().get("exp-a").get("__expired__")).isEqualTo(2);
        assertThat(bucketer.snapshot().get("exp-a").get("__unenrolled__")).isNull(); // 口径不混
    }

    @Test
    void noExpiryDeclarationIsUnaffected() {
        MutableClock clock = new MutableClock();
        ExperimentBucketer bucketer = new ExperimentBucketer(
                Map.of("exp-b", Map.of("control", 100)),
                Map.of(),
                clock);
        clock.advanceSeconds(365L * 24 * 3600);
        assertThat(bucketer.assign("exp-b", "u1")).isEqualTo("control");
        assertThat(bucketer.expiredExperiments()).isEmpty();
        assertThat(bucketer.expiresAt("exp-b")).isEqualTo(Optional.empty());
        assertThat(bucketer.expiresAt("unknown")).isEqualTo(Optional.empty());
    }

    @Test
    void nullExpiryInstantFailsFast() {
        Map<String, Instant> withNull = new java.util.HashMap<>();
        withNull.put("exp-c", null); // HashMap 允许 null 值——校验由构造器负责
        assertThatThrownBy(() -> new ExperimentBucketer(
                Map.of("exp-c", Map.of("t", 10)),
                withNull,
                Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
