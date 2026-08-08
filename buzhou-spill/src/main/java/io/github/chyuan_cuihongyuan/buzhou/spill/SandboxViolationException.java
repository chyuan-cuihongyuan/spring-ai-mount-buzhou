package io.github.chyuan_cuihongyuan.buzhou.spill;

/**
 * @deprecated 已上移至 {@link io.github.chyuan_cuihongyuan.buzhou.core.fs.SandboxViolationException}
 *             （ticket 16：沙箱归 core 公共包，feature 模块共享）；本壳类仅为兼容保留。
 */
@Deprecated(forRemoval = false)
public class SandboxViolationException
        extends io.github.chyuan_cuihongyuan.buzhou.core.fs.SandboxViolationException {

    public SandboxViolationException(String message) {
        super(message);
    }
}
