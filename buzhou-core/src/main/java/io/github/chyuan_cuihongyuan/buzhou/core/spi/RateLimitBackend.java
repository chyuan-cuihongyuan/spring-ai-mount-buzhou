package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * 限流后端 SPI（spec 54 §A / T222 / effort#14，core.spi——resilience 策略层与
 * store-redis 实现层共用）：策略（排队/拒绝/事件）留在 resilience 的
 * {@code ModelRateLimiter}，额度存取抽象到后端——单进程部署用内存令牌桶
 * （{@code InMemoryRateLimitBackend}，默认，行为零变化），多实例共享额度用
 * Redis 固定窗（分钟窗 INCR/EXPIRE，LiteLLM 同款；整形特性差异诚实入档）。
 *
 * <p>维度：调用方约定字符串（"RPM"/"TPM"，常量在 resilience 侧 ModelRateLimiter）。
 * 实现必须线程安全；Redis 实现故障语义 = fail-fast 上抛（不静默 fail-open）。
 */
public interface RateLimitBackend {

    /** 尝试扣减（amount ≤ 0 为纯预检不扣，预检 = 仍有额度才放行）；成功 true / 额度不足 false。 */
    boolean tryAcquire(String modelName, String dimension, double amount);

    /** 事后记账（TPM usage 口径；可致余额为负——诚实表达超限，下次预检拒绝）。 */
    void consume(String modelName, String dimension, double amount);

    /** 当前可用额度（≥0 封顶容量；无桶 = 容量满）。 */
    double available(String modelName, String dimension);

    /** 维度容量（未启用维度 = 0）。 */
    double capacity(String dimension);

    /** 补充到 amount 可用还需秒数（已可用 = 0；不可知 = 大值由策略超时兜底）。 */
    double secondsUntilAvailable(String modelName, String dimension, double amount);

    /** 后端标识（观测/日志：memory / redis）。 */
    String kind();
}
