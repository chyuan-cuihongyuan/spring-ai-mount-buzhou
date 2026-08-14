package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;

/** 沙箱越界/解析失败异常（spec 06 安全边界）。携带 {@link ErrorCode#SANDBOX_VIOLATION}（不可重试）。 */
public class SandboxViolationException extends BuzhouException {

    private static final long serialVersionUID = 1L;

    public SandboxViolationException(String message) {
        super(ErrorCode.SANDBOX_VIOLATION, message);
    }
}
