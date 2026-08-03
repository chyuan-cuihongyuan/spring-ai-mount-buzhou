package io.github.chyuan_cuihongyuan.buzhou.core.observability;

/**
 * Span 终态。{@link #RUNNING} 为开启后落库的中间态，关闭时 upsert 为终态。
 */
public final class SpanStatus {

    public static final String RUNNING = "RUNNING";
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
    public static final String CANCELLED = "CANCELLED";

    private SpanStatus() {
    }
}
