package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

/**
 * 模型自限流拒绝异常（spec「背压 · 维度③ 模型 RPM+TPM 双桶」）。
 *
 * <p>当模型调用被框架自限流拒绝（RPM/TPM 桶空且过载策略为 FAIL_FAST，
 * 或 QUEUE 档排队超时）时抛出。**不进入重试分类**——直接上抛（与 provider 429 的
 * 可重试语义严格区分，避免重试放大拥塞）。
 *
 * <p>用户可在 {@code onModelError} Hook 切面对本异常做兜底（Replace/Block），
 * 与 provider 侧失败走同一切面但类型可区分。
 */
public class ModelRateLimitExceededException extends RuntimeException {

    private final String modelName;
    private final String dimension;  // "RPM" | "TPM"
    private final long waitedMillis;

    /**
     * @param modelName 模型名（分桶键）
     * @param dimension 桶维度（RPM / TPM）
     * @param waited    排队已等待时长（FAIL_FAST 档为 {@link java.time.Duration#ZERO}）
     */
    public ModelRateLimitExceededException(String modelName, String dimension, java.time.Duration waited) {
        super("Model rate limit exceeded: model=" + modelName
                + ", dimension=" + dimension
                + ", waitedMs=" + (waited == null ? 0 : waited.toMillis()));
        this.modelName = modelName;
        this.dimension = dimension;
        this.waitedMillis = waited == null ? 0 : waited.toMillis();
    }

    public String modelName() {
        return modelName;
    }

    public String dimension() {
        return dimension;
    }

    public long waitedMillis() {
        return waitedMillis;
    }
}
