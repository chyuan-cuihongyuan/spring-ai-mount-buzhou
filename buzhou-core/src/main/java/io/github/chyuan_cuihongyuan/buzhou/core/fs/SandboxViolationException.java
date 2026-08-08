package io.github.chyuan_cuihongyuan.buzhou.core.fs;

/** 沙箱越界/解析失败异常（spec 06 安全边界）。 */
public class SandboxViolationException extends RuntimeException {

    public SandboxViolationException(String message) {
        super(message);
    }
}
