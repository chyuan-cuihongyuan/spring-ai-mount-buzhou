package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

/**
 * spawn 排队优先级（spec 123 / T445，OS 调度多级队列 + Envoy 优先级面借鉴）：
 * 释放的空位有向交接给<b>最高非空级</b>的队首——高级抢占低级排队者、同级严格 FIFO。
 *
 * <p>语义：HIGH（运维接管/关键租户）＞ NORMAL（默认——既有单参调用等价）＞
 * LOW（批处理/离线会话）。FAIL_FAST 档不排队，优先级无意义。
 */
public enum SpawnPriority {
    /** 最高优先：空位释放即插队（在 NORMAL/LOW 排队者之前）。 */
    HIGH,
    /** 默认优先：既有 {@code acquireSlotOrThrow(sessionId)} 等价档。 */
    NORMAL,
    /** 最低优先：让交互会话在争用时先行。 */
    LOW
}
