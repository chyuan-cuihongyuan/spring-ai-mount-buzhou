package io.github.chyuan_cuihongyuan.buzhou.core.error;

/**
 * Buzhou 统一异常基类（spec 13 §cross-11 / ticket 29）：携带结构化 {@link ErrorCode}
 * （内含 {@link RetryCategory} 分类），供告警、自动化策略与上层重试框架按类别决策。
 *
 * <p>领域异常（SandboxViolationException / LeaseLostException / SessionAlreadyActiveException /
 * QuotaExceededException / BuzhouDataCorruptionException 等）继承本类；基类不 sealed，
 * 各机制模块可按需扩展自己的领域异常（保留类名与构造签名兼容）。
 */
public class BuzhouException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /**
     * 以错误码与具体消息构造。
     *
     * @param errorCode 结构化错误码（不允许 null）
     * @param message   具体异常消息
     */
    public BuzhouException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = requireErrorCode(errorCode);
    }

    /**
     * 以错误码、具体消息与根因构造。
     *
     * @param errorCode 结构化错误码（不允许 null）
     * @param message   具体异常消息
     * @param cause     根因异常
     */
    public BuzhouException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = requireErrorCode(errorCode);
    }

    private static ErrorCode requireErrorCode(ErrorCode errorCode) {
        if (errorCode == null) {
            throw new IllegalArgumentException("ErrorCode must not be null");
        }
        return errorCode;
    }

    /** 携带的结构化错误码。 */
    public ErrorCode errorCode() {
        return errorCode;
    }

    /** 便捷访问：错误码的重试分类（等价于 {@code errorCode().retryCategory()}）。 */
    public RetryCategory retryCategory() {
        return errorCode.retryCategory();
    }
}
