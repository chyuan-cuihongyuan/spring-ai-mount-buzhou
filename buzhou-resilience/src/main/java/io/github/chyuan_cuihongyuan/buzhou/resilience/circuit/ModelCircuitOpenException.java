package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import java.time.Duration;

/**
 * 熔断器 OPEN 期快速失败（spec 15「熔断器」）：不进重试分类（跳闸后重试只会继续锤故障 provider），
 * 与自限流拒绝（{@code ModelRateLimitExceededException}）同语义——直上 {@code onModelError}。
 */
public final class ModelCircuitOpenException extends RuntimeException {

    private final String modelName;
    private final CircuitState state;
    private final Duration retryIn;

    public ModelCircuitOpenException(String modelName, CircuitState state, Duration retryIn) {
        super("模型熔断器拒绝调用（model=" + modelName + "，state=" + state
                + (retryIn != null ? "，剩余冷却 " + retryIn.toMillis() + "ms" : "") + "）：快速失败，不重试");
        this.modelName = modelName;
        this.state = state;
        this.retryIn = retryIn;
    }

    public String modelName() {
        return modelName;
    }

    public CircuitState state() {
        return state;
    }

    /** OPEN 态剩余冷却；HALF_OPEN 探测占位时为 null（稍后重试）。 */
    public Duration retryIn() {
        return retryIn;
    }
}
