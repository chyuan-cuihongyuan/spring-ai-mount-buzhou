package io.github.chyuan_cuihongyuan.buzhou.core.observability;

/**
 * Span 种类：核心四类 + Harness 内部动作（spec 03 数据模型）。
 *
 * <p>用字符串常量而非 enum，与既有 {@code SpanRecord.kind(String)} SPI 落地形态一致；
 * {@code HARNESS_INTERNAL} 挂压缩/Spill/Hook/修复/HITL 等框架内部动作于所属 Turn 下。
 */
public final class SpanKind {

    public static final String SESSION = "SESSION";
    public static final String TURN = "TURN";
    public static final String MODEL_CALL = "MODEL_CALL";
    public static final String TOOL_CALL = "TOOL_CALL";
    public static final String HARNESS_INTERNAL = "HARNESS_INTERNAL";

    private SpanKind() {
    }
}
