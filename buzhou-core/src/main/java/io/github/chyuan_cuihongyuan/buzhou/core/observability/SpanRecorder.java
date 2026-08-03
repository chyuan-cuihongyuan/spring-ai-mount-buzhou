package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import java.util.Map;

/**
 * 采集入口：各机制（Hook、压缩、Spill、advisor、ToolCallback 包装）经此开 span / 发 Event。
 *
 * <p>实现负责把记录推入异步管线（背压不丢）或同步直写（测试用），采集方不感知落库细节。
 */
public interface SpanRecorder {

    /** 开 span；{@code parent} 可为 null（根 Session span）。 */
    SpanHandle openSpan(String kind, String name, SpanContext parent);

    /** 开 span 并写入初始属性袋。 */
    SpanHandle openSpan(String kind, String name, SpanContext parent, Map<String, Object> attributes);

    /** 开 span 并显式指定 sessionId（用于根 Session span，parent=null 时无法从 parent 继承）。 */
    SpanHandle openSpan(String kind, String name, SpanContext parent, Map<String, Object> attributes,
                        SpanContext explicitContext);

    /** 在已存在的 span（由 SpanContext 标识）下发 Event。 */
    void emit(SpanContext span, String type, Map<String, Object> payload);

    /** 强制把在途记录批量落库（会话 close / JVM shutdown 调用）。 */
    void flush();
}
