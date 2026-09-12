package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GCRA 平滑限流后端测试（spec 603 / T856–T857 / impl 456）：严格平滑步调、突发容忍、
 * 预检不推进、consume 超限强推、等待秒数、available 近似口径、未启用维度与参数校验。
 */
class GcraRateLimitBackendTest {

    /** 可推进 nano 时钟（确定性测试）。 */
    private static final class MutableNanoClock {
        private final AtomicLong nanos = new AtomicLong();

        long advanceSec(double sec) {
            return nanos.addAndGet((long) (sec * 1_000_000_000L));
        }

        LongSupplier nano() {
            return nanos::get;
        }
    }

    /** cap=6/分钟 → τ=10s。 */
    private static GcraRateLimitBackend backend(MutableNanoClock clock, Duration burst) {
        return new GcraRateLimitBackend(6, 1000, burst, clock.nano());
    }

    /** 严格平滑（β=0）：t0 放行一个，即刻第二个拒绝，满 τ 后再放行。 */
    @Test
    void strictPacingWithoutBurst() {
        MutableNanoClock clock = new MutableNanoClock();
        GcraRateLimitBackend backend = backend(clock, Duration.ZERO);

        assertThat(backend.tryAcquire("m", "RPM", 1)).isTrue();  // tat = t0+10
        assertThat(backend.tryAcquire("m", "RPM", 1)).isFalse(); // tat−t0 = 10 > β=0
        clock.advanceSec(10);
        assertThat(backend.tryAcquire("m", "RPM", 1)).isTrue();
    }

    /** 突发容忍 β=30s：即刻连发最多 4 个（tat−t ≤ 30），第 5 个拒绝。 */
    @Test
    void burstToleranceAdmitsBoundedBurst() {
        MutableNanoClock clock = new MutableNanoClock();
        GcraRateLimitBackend backend = backend(clock, Duration.ofSeconds(30));

        for (int i = 0; i < 4; i++) {
            assertThat(backend.tryAcquire("m", "RPM", 1)).as("第 " + (i + 1) + " 个应放行").isTrue();
        }
        assertThat(backend.tryAcquire("m", "RPM", 1)).isFalse(); // tat−t0 = 40 > 30
    }

    /** 纯预检（amount≤0）不推进 TAT：预检后额度不受影响。 */
    @Test
    void probeDoesNotAdvanceTat() {
        MutableNanoClock clock = new MutableNanoClock();
        GcraRateLimitBackend backend = backend(clock, Duration.ZERO);

        assertThat(backend.tryAcquire("m", "RPM", 0)).isTrue();
        assertThat(backend.tryAcquire("m", "RPM", 0)).isTrue(); // 反复预检恒同果
        assertThat(backend.tryAcquire("m", "RPM", 1)).isTrue(); // 首个真实扣减仍可用
    }

    /** consume 超限强推 TAT（诚实超限）：随后预检拒绝，时间追上后恢复。 */
    @Test
    void consumeOverdrawsThenRecovers() {
        MutableNanoClock clock = new MutableNanoClock();
        GcraRateLimitBackend backend = backend(clock, Duration.ZERO);

        backend.consume("m", "RPM", 600); // 强推 600×10s = 6000s
        assertThat(backend.tryAcquire("m", "RPM", 0)).isFalse();
        clock.advanceSec(6000);
        assertThat(backend.tryAcquire("m", "RPM", 0)).isTrue();
    }

    /** 等待秒数：β=0 首个放行后，下一额度需等 τ。 */
    @Test
    void secondsUntilAvailableMatchesTau() {
        MutableNanoClock clock = new MutableNanoClock();
        GcraRateLimitBackend backend = backend(clock, Duration.ZERO);

        assertThat(backend.tryAcquire("m", "RPM", 1)).isTrue();
        assertThat(backend.secondsUntilAvailable("m", "RPM", 1)).isEqualTo(10.0);
    }

    /** available 近似口径：「此刻还能连发几个」——β=30 新桶 4 个、超限 0、封顶容量。 */
    @Test
    void availableApproximationClamped() {
        MutableNanoClock clock = new MutableNanoClock();
        GcraRateLimitBackend backend = backend(clock, Duration.ofSeconds(30));

        assertThat(backend.available("m", "RPM")).isEqualTo(4.0); // floor(30/10)+1，与突发测试口径一致
        backend.consume("m", "RPM", 600);
        assertThat(backend.available("m", "RPM")).isZero();
    }

    /** 未启用维度：恒拒、容量 0、等待不可知；kind 标识 memory-gcra。 */
    @Test
    void unconfiguredDimensionDeniedAndKindLabel() {
        MutableNanoClock clock = new MutableNanoClock();
        GcraRateLimitBackend backend = new GcraRateLimitBackend(6, null, Duration.ZERO, clock.nano());

        assertThat(backend.tryAcquire("m", "TPM", 1)).isFalse();
        assertThat(backend.capacity("TPM")).isZero();
        assertThat(backend.secondsUntilAvailable("m", "TPM", 1)).isEqualTo(Double.MAX_VALUE);
        assertThat(backend.kind()).isEqualTo("memory-gcra");
    }

    /** burstTolerance 负数构造拒绝。 */
    @Test
    void negativeBurstToleranceRejected() {
        MutableNanoClock clock = new MutableNanoClock();
        assertThatThrownBy(() -> new GcraRateLimitBackend(6, 1000, Duration.ofSeconds(-1), clock.nano()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
