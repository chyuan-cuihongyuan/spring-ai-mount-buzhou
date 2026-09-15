package io.github.chyuan_cuihongyuan.buzhou.observability;

import java.util.concurrent.atomic.AtomicLong;

/**
 * span 属性预算审计（L 会话 1700 系 R33 = effort #1732 / spec 1732 /
 * 票 T2665 + T2666 / impl 1332）——OpenTelemetry 的 span 属性限额思想
 * （默认 128 属性/总字节上限）：属性数与体积超预算的 span 是管道内存
 * 与后端基数的隐形杀手——「哪些 span 在超预算」须有账。
 *
 * <p>实例面线程安全：`record(spanName, attrCount, attrBytes)` 逐 span
 * 记账（命名超长截断 64 字符——基数纪律），超限按可调阈值（默认 128 属性/
 * 8192 字节）计数 +census（超限span/最大属性数/最大字节）。
 * 纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class SpanAttributeBudget {

    /** 默认属性数上限（OTel 默认 128）。 */
    public static final int DEFAULT_MAX_ATTRS = 128;

    /** 默认属性总字节上限。 */
    public static final int DEFAULT_MAX_BYTES = 8192;

    private final int maxAttrs;
    private final int maxBytes;
    private final AtomicLong spans = new AtomicLong();
    private final AtomicLong overAttr = new AtomicLong();
    private final AtomicLong overByte = new AtomicLong();
    private final AtomicLong worstAttrs = new AtomicLong();
    private final AtomicLong worstBytes = new AtomicLong();

    /** 默认阈值。 */
    public SpanAttributeBudget() {
        this(DEFAULT_MAX_ATTRS, DEFAULT_MAX_BYTES);
    }

    /** 自定义阈值（&lt;1 按默认）。 */
    public SpanAttributeBudget(int maxAttrs, int maxBytes) {
        this.maxAttrs = maxAttrs < 1 ? DEFAULT_MAX_ATTRS : maxAttrs;
        this.maxBytes = maxBytes < 1 ? DEFAULT_MAX_BYTES : maxBytes;
    }

    /** 记一个 span 的属性规模。 */
    public void record(String spanName, int attrCount, int attrBytes) {
        spans.incrementAndGet();
        worstAttrs.getAndUpdate(prev -> Math.max(prev, attrCount));
        worstBytes.getAndUpdate(prev -> Math.max(prev, Math.max(attrBytes, 0)));
        if (attrCount > maxAttrs) {
            overAttr.incrementAndGet();
        }
        if (attrBytes > maxBytes) {
            overByte.incrementAndGet();
        }
    }

    /**
     * @param spans       记账 span 数
     * @param overAttrLimit 属性数超限 span 数
     * @param overByteLimit 字节超限 span 数
     * @param worstAttrs  最大属性数
     * @param worstBytes  最大属性字节
     */
    public record BudgetCensus(long spans, long overAttrLimit, long overByteLimit,
                               long worstAttrs, long worstBytes) {
    }

    /** 快照。 */
    public BudgetCensus census() {
        return new BudgetCensus(spans.get(), overAttr.get(), overByte.get(),
                worstAttrs.get(), worstBytes.get());
    }
}
