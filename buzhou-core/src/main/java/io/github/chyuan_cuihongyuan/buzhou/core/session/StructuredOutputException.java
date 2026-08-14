package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;

/**
 * 结构化输出解析失败（spec 19 / T87 / impl-62）：REASK 一次后模型输出仍无法解析为
 * 目标类型。NON_RETRYABLE——同一会话原样重试大概率再失败，应修正提示词 / 目标类型或换模型。
 */
public class StructuredOutputException extends BuzhouException {

    private static final long serialVersionUID = 1L;

    public StructuredOutputException(String message, Throwable cause) {
        super(ErrorCode.STRUCTURED_OUTPUT_FAILED, message, cause);
    }
}
