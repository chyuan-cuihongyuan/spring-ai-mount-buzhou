package io.github.chyuan_cuihongyuan.buzhou.core.error;

/**
 * 配额超额异常（spec 13 §growth-8 / ticket 29 占位落地）：事实台账等不可牺牲集合
 * （message/summary/state）超额时<b>明确拒绝而非静默丢弃</b>（Redis noeviction 语义）。
 *
 * <p>分类 {@link RetryCategory#NON_RETRYABLE}：原样重试必然再超额，需释放空间或提升配额后另行发起。
 */
public class QuotaExceededException extends BuzhouException {

    private static final long serialVersionUID = 1L;

    /** 以具体配额消息构造（如「per-session 消息数达到上限 5000」）。 */
    public QuotaExceededException(String message) {
        super(ErrorCode.QUOTA_EXCEEDED, message);
    }

    /** 以具体配额消息与根因构造。 */
    public QuotaExceededException(String message, Throwable cause) {
        super(ErrorCode.QUOTA_EXCEEDED, message, cause);
    }
}
