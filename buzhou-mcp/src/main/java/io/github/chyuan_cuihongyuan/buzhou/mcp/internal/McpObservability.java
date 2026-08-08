package io.github.chyuan_cuihongyuan.buzhou.mcp.internal;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.EventType;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanRecorder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 热插拔的可观测出口（spec 04：全部内部动作产 HarnessInternal Span + Event）。
 *
 * <p>事件类型名按 spec 原文（小数点层级）：{@code mcp.added / mcp.removed / mcp.closed /
 * mcp.forceClosed}，经 {@link EventType#of} 注册进开放枚举。Span 为 HarnessInternal
 * {@code mcp.refresh}，无会话归属（harness 全局动作），parent=null。
 *
 * <p>recorder 可空：模块独立可用（只引 buzhou-mcp 不强依赖可观测管线），空时全部静默。
 */
public final class McpObservability {

    public static final String SPAN_REFRESH = "mcp.refresh";
    public static final String MCP_ADDED = "mcp.added";
    public static final String MCP_REMOVED = "mcp.removed";
    public static final String MCP_CLOSED = "mcp.closed";
    public static final String MCP_FORCE_CLOSED = "mcp.forceClosed";

    static {
        EventType.of(MCP_ADDED);
        EventType.of(MCP_REMOVED);
        EventType.of(MCP_CLOSED);
        EventType.of(MCP_FORCE_CLOSED);
    }

    private final SpanRecorder recorder;

    public McpObservability(SpanRecorder recorder) {
        this.recorder = recorder;
    }

    /** 开 refresh span；recorder 为空返回 null（下游全部判空跳过）。 */
    public SpanHandle openRefreshSpan(Map<String, Object> attributes) {
        if (recorder == null) {
            return null;
        }
        return recorder.openSpan(SpanKind.HARNESS_INTERNAL, SPAN_REFRESH, null, attributes);
    }

    public void added(SpanContext span, String server, String reason) {
        emit(span, MCP_ADDED, server, reason);
    }

    public void removed(SpanContext span, String server, String reason) {
        emit(span, MCP_REMOVED, server, reason);
    }

    public void closed(SpanContext span, String server, String reason) {
        emit(span, MCP_CLOSED, server, reason);
    }

    /** 强杀兜底（spec 04：Error Event + Span 标记）——payload 带 error 标记，span 置 ERROR 属性。 */
    public void forceClosed(SpanContext span, String server) {
        if (recorder == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("server", server);
        payload.put("error", true);
        payload.put("reason", "close-timeout");
        recorder.emit(span, MCP_FORCE_CLOSED, payload);
    }

    /** 建连失败（推演：spec 未定该事件形态，复用核心 ERROR 类型，phase=connect）。 */
    public void connectFailed(SpanContext span, String server, Throwable t) {
        emitError(span, "connect", server, t);
    }

    /** 物理 close 抛异常（推演 13：reason 闭集不篡改，失败单发 ERROR，phase=close）。 */
    public void closeFailed(SpanContext span, String server, Throwable t) {
        emitError(span, "close", server, t);
    }

    /** 坏配置（如清单重名）整批拒绝生效；recorder 可空。无 span 可挂，span=null 直发。 */
    public static void refreshRejected(SpanRecorder recorder, RuntimeException e) {
        if (recorder == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("phase", "refresh");
        payload.put("exception.type", e.getClass().getName());
        payload.put("exception.message", String.valueOf(e.getMessage()));
        recorder.emit(null, EventType.ERROR, payload);
    }

    private void emitError(SpanContext span, String phase, String server, Throwable t) {
        if (recorder == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("server", server);
        payload.put("phase", phase);
        payload.put("exception.type", t.getClass().getName());
        payload.put("exception.message", String.valueOf(t.getMessage()));
        recorder.emit(span, EventType.ERROR, payload);
    }

    private void emit(SpanContext span, String type, String server, String reason) {
        if (recorder == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("server", server);
        if (reason != null) {
            payload.put("reason", reason);
        }
        recorder.emit(span, type, payload);
    }
}
