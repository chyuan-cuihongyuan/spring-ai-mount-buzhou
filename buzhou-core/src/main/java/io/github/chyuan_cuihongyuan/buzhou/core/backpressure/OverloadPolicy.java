package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

/**
 * 过载处置策略（spec 15「背压与多层限流 · 过载语义两档」）。
 *
 * <p>三维背压（spawn 闸 / 模型双桶限流）共用此枚举，每维度独立可配。
 *
 * <ul>
 *   <li>{@link #QUEUE} —— 有界排队 + 超时（默认）：超限时不立即拒绝，排队等待空位 / 令牌，
 *       带超时兜底；突发流量被削峰而非直接拒绝。</li>
 *   <li>{@link #FAIL_FAST} —— 快速失败：超限时立即拒绝，不排队；强实时场景不被排队延迟拖累。</li>
 * </ul>
 *
 * <p>语义契约：拒绝 = 调用方可重试的明确异常 + 事件进 observability 既有通道。
 */
public enum OverloadPolicy {

    /** 有界排队 + 超时（默认档）。 */
    QUEUE,

    /** 快速失败（不排队，立即拒绝）。 */
    FAIL_FAST
}
