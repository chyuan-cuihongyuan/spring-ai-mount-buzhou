package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * 消息历史读失败降级策略（spec 42 §B / T156 / impl-127）。
 *
 * <ul>
 *   <li>{@link #OFF}（默认）：读失败原样上抛——Turn 失败，与既往行为一致；</li>
 *   <li>{@link #EMPTY}：读失败降级为空历史继续本轮——会话保活（模型看不到历史），WARN 日志 +
 *       {@code buzhou.stores.read-degraded}（outcome=empty）计数可感，不静默。</li>
 * </ul>
 *
 * @since 1.0.0
 */
public enum ReadDegradePolicy {

    OFF,
    EMPTY
}
