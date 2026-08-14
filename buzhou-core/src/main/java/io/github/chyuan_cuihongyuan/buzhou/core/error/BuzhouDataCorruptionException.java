package io.github.chyuan_cuihongyuan.buzhou.core.error;

/**
 * 数据损坏异常（spec 13 §stores-7 / ticket 29 占位落地）：单条持久化记录无法解析
 * （脏 JSON / 链校验断点）时标记该记录；加载路径按「跳过 + WARN + 计数」隔离，
 * 绝不炸掉整个会话，计数达到阈值由运维侧介入修复。
 *
 * <p>分类 {@link RetryCategory#FATAL}：损坏不会自愈，重试无意义，需人工修复数据。
 */
public class BuzhouDataCorruptionException extends BuzhouException {

    private static final long serialVersionUID = 1L;

    /** 以具体损坏描述构造（含定位信息，如 sessionId + 记录键）。 */
    public BuzhouDataCorruptionException(String message) {
        super(ErrorCode.DATA_CORRUPTION, message);
    }

    /** 以具体损坏描述与解析根因构造。 */
    public BuzhouDataCorruptionException(String message, Throwable cause) {
        super(ErrorCode.DATA_CORRUPTION, message, cause);
    }
}
