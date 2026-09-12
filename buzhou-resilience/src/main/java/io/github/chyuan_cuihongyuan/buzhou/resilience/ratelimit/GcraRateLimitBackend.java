package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.RateLimitBackend;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * GCRA 平滑限流后端（spec 603 / T856，redis-cell / Envoy GCRA 借鉴；opt-in——
 * 默认仍是 {@link InMemoryRateLimitBackend} 令牌桶，行为零变化）。
 *
 * <p><b>与令牌桶的整形差异（诚实入档）</b>：令牌桶容量即突发额度（开局可连打
 * capacity 次）；GCRA 以 TAT（理论到达时刻）匀速排队——默认突发容忍 0 = 严格
 * 平滑（每 {@code 60/capacity} 秒放行一个），供应商按平滑速率计 RPM 的场景
 * （突发即 429）选它。需要有限突发用
 * {@link #GcraRateLimitBackend(Integer, Integer, Duration)} 给 burstTolerance。
 *
 * <p>多单元（TPM 记账 amount&gt;1）按同瞬连发 n 个 cell 处理：接受条件
 * {@code tat − now ≤ β + (n−1)·τ}，接受后 {@code tat += n·τ}；{@link #consume}
 * 强推 TAT（对应令牌桶负余额的「诚实超限」——TAT 越界后预检拒绝直至时间追上）。
 */
public final class GcraRateLimitBackend implements RateLimitBackend {

    private static final double SECONDS_PER_MINUTE = 60.0;

    private final double rpmCapacity;
    private final double tpmCapacity;
    /** 突发容忍 β（秒；0 = 严格平滑）。 */
    private final double burstToleranceSec;
    private final LongSupplier nanoClock;
    private final ConcurrentHashMap<String, TatState> states = new ConcurrentHashMap<>();

    public GcraRateLimitBackend(Integer rpm, Integer tpm) {
        this(rpm, tpm, Duration.ZERO);
    }

    public GcraRateLimitBackend(Integer rpm, Integer tpm, Duration burstTolerance) {
        this(rpm, tpm, burstTolerance, System::nanoTime);
    }

    /** 测试注入时钟构造（nanoTime 单调语义）。 */
    GcraRateLimitBackend(Integer rpm, Integer tpm, Duration burstTolerance, LongSupplier nanoClock) {
        this.rpmCapacity = rpm != null && rpm > 0 ? rpm : 0;
        this.tpmCapacity = tpm != null && tpm > 0 ? tpm : 0;
        if (burstTolerance == null || burstTolerance.isNegative()) {
            throw new IllegalArgumentException("burstTolerance 必须非负（当前 " + burstTolerance + "）");
        }
        this.burstToleranceSec = burstTolerance.toMillis() / 1000.0;
        this.nanoClock = nanoClock;
    }

    @Override
    public boolean tryAcquire(String modelName, String dimension, double amount) {
        TatState state = stateOf(modelName, dimension);
        double tau = tauOf(dimension);
        synchronized (state) {
            double nowSec = nowSec();
            if (tau <= 0) {
                return false; // 未启用维度恒拒（对齐 InMemory 0 容量语义，不抛）
            }
            if (amount <= 0) {
                return state.tatSec - nowSec <= burstToleranceSec; // 纯预检不推进 TAT
            }
            double units = Math.ceil(amount);
            boolean admissible = state.tatSec - nowSec
                    <= burstToleranceSec + (units - 1) * tau;
            if (admissible) {
                state.tatSec = Math.max(state.tatSec, nowSec) + units * tau;
            }
            return admissible;
        }
    }

    @Override
    public void consume(String modelName, String dimension, double amount) {
        TatState state = stateOf(modelName, dimension);
        double tau = tauOf(dimension);
        if (tau <= 0) {
            return;
        }
        synchronized (state) {
            state.tatSec = Math.max(state.tatSec, nowSec()) + Math.max(0, Math.ceil(amount)) * tau;
        }
    }

    @Override
    public double available(String modelName, String dimension) {
        TatState state = stateOf(modelName, dimension);
        double tau = tauOf(dimension);
        if (tau <= 0) {
            return 0;
        }
        synchronized (state) {
            // GCRA 无离散 token：「此刻起还能连发几个」= floor((now+β−tat)/τ)+1（负则 0），封顶容量
            double admissible = Math.floor((nowSec() + burstToleranceSec - state.tatSec) / tau) + 1;
            return Math.clamp(admissible, 0, capacity(dimension));
        }
    }

    @Override
    public double capacity(String dimension) {
        return switch (dimension) {
            case ModelRateLimiter.DIMENSION_RPM -> rpmCapacity;
            case ModelRateLimiter.DIMENSION_TPM -> tpmCapacity;
            default -> 0;
        };
    }

    @Override
    public double secondsUntilAvailable(String modelName, String dimension, double amount) {
        TatState state = stateOf(modelName, dimension);
        double tau = tauOf(dimension);
        if (tau <= 0) {
            return Double.MAX_VALUE;
        }
        synchronized (state) {
            double units = Math.max(1, Math.ceil(amount));
            double waitSec = state.tatSec - nowSec() - burstToleranceSec + (units - 1) * tau;
            return Math.max(0, waitSec);
        }
    }

    @Override
    public String kind() {
        return "memory-gcra";
    }

    /** 发射间隔 τ（秒）= 60 / 容量（每分钟容量换算每秒速率的倒数）。 */
    private double tauOf(String dimension) {
        double cap = capacity(dimension);
        return cap > 0 ? SECONDS_PER_MINUTE / cap : 0;
    }

    private TatState stateOf(String modelName, String dimension) {
        return states.computeIfAbsent(modelName + ":" + dimension, k -> new TatState(nowSec()));
    }

    private double nowSec() {
        return nanoClock.getAsLong() / 1_000_000_000.0;
    }

    /** TAT 状态（秒域；由调用方 synchronized 互斥——与 TokenBucket 同并发风格）。 */
    private static final class TatState {
        double tatSec;

        TatState(double initialSec) {
            this.tatSec = initialSec;
        }
    }
}
