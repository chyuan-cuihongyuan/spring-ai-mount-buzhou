package io.github.chyuan_cuihongyuan.buzhou.core.error;

/**
 * 异常重试分类（spec 13 §cross-11 / ticket 29）：每个 {@link ErrorCode} 携带一个类别，
 * 供告警、自动化策略与上层重试框架按类别决策——「重试能否改变结局」是唯一判据。
 *
 * <p>类别语义（对自动化策略的含义）：
 * <ul>
 *   <li>{@link #RETRYABLE}：瞬态故障（超时、存储抖动、停机中断窗口）——原样或退避后重试可能成功；</li>
 *   <li>{@link #NON_RETRYABLE}：输入或状态被拒（参数校验失败、配额超额、会话已激活、租约丢失）——
 *       原样重试必然再失败，需修正输入 / 释放资源 / 换会话后再发起；</li>
 *   <li>{@link #FATAL}：环境或数据根因（配置非法、数据损坏）——重试无意义，需人工介入修复。</li>
 * </ul>
 */
public enum RetryCategory {

    /** 瞬态故障：重试可能成功（超时、存储写失败、停机中断等）。 */
    RETRYABLE,

    /** 不可重试：重试必然再失败，需修正前提后另行发起（校验失败、配额超额、租约丢失等）。 */
    NON_RETRYABLE,

    /** 致命：重试无意义，需人工介入（配置非法、数据损坏等）。 */
    FATAL
}
