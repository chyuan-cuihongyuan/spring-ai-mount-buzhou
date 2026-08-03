package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import java.util.Map;

/**
 * Span 句柄：开 span 后返回，采集方累积属性、收尾关闭（spec 03 采集 API）。
 *
 * <p>{@link #attribute(String, Object)} 累积进属性袋；{@link #error(Throwable)} 置 ERROR 状态并发
 * ERROR Event；{@link #close()} 设 endTime + 终态（默认 RUNNING→OK）并 enqueue upsert。
 * 实现 MUST 线程安全（同一句柄可能被 advisor 主链路与并发工具回调持有）。
 */
public interface SpanHandle extends AutoCloseable {

    SpanContext context();

    SpanHandle attribute(String key, Object value);

    SpanHandle attributes(Map<String, Object> attributes);

    /** 置 ERROR 状态 + 发 ERROR Event（payload 含 exception.type/message，stacktrace 受配置开关）。 */
    void error(Throwable t);

    /** 显式终态关闭（CANCELLED 等）；默认 {@link #close()} 走 RUNNING→OK。 */
    void close(String status);

    @Override
    void close();
}
