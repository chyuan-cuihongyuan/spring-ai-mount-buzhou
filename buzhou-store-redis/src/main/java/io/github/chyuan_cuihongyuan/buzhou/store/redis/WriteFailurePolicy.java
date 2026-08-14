package io.github.chyuan_cuihongyuan.buzhou.store.redis;

/**
 * 存储写失败策略（spec 13 §stores-7 / ticket 32，{@code buzhou.store.write-failure-policy}）。
 *
 * <ul>
 *   <li>{@link #FAIL_TURN}（默认）：写失败原样抛出——既有「外溢语义」，Turn 因存储故障失败；</li>
 *   <li>{@link #DEGRADE}：<b>只对可再生数据生效</b>——观测类写（span / event / 注入快照，
 *       可由下一轮重建或仅损失可观测性）降级为 WARN + 计数继续；事实类写
 *       （message / summary / state / lease，事实台账不可静默丢）<b>仍照常抛出</b>。</li>
 * </ul>
 *
 * <p>与 buzhou-store-jdbc 的同名枚举为等价声明（模块间不互相依赖，
 * 与 RedisJson/JdbcJson 的既有惯例一致）。
 */
public enum WriteFailurePolicy {

    /** 写失败原样抛（默认 = 既有外溢语义）。 */
    FAIL_TURN,

    /** 观测类写降级（WARN + 计数继续）；事实类写不受影响仍抛。 */
    DEGRADE
}
