package io.github.chyuan_cuihongyuan.buzhou.core.hook;

/**
 * 读写两侧失败语义的统一动词汇（wayfinder T19 / docs/spec/11 guard；来源 Guardrails AI `on_fail`）。
 *
 * <p>给既有「读写失败非对称」套上业界心智模型，<b>不改其语义</b>：
 *
 * <ul>
 *   <li>{@link #FILTER}——读侧默认：降级继续（透传原文/替代物），不阻断；对应既有「读降级」
 *       （如 offload 落盘失败透传原文，模型承担自截断风险）。</li>
 *   <li>{@link #REFRAIN}——读侧保守降级：以「拒答该数据」文本替代原文（不给可能残缺/不可信的数据）。</li>
 *   <li>{@link #EXCEPTION}——写侧默认：阻断（BLOCK），不外流残缺产物；对应既有「写阻断」
 *       （onload 失败 / 副本分离拦截 / HITL 未授权）。</li>
 *   <li>{@link #REASK}——可恢复失败：错误回喂模型自我纠错重试（对应 T16「错误即反馈」通道），
 *       <b>有上界</b>（由 T17 有界 Turn 的递归预算兜底，不无限循环）。</li>
 * </ul>
 */
public enum OnFail {

    /** 读侧：降级继续（透传/替代物），不阻断。 */
    FILTER,

    /** 读侧：保守降级，以「拒答该数据」文本替代。 */
    REFRAIN,

    /** 写侧：阻断（BLOCK），不外流残缺产物。 */
    EXCEPTION,

    /** 可恢复失败：错误回喂模型重试（有上界）。 */
    REASK
}
