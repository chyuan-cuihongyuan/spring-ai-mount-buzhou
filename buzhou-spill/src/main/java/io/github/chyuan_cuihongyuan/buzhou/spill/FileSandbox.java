package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.nio.file.Path;
import java.util.List;

/**
 * @deprecated 实现已上移至 {@link io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox}
 *             （ticket 16：沙箱归 core 公共包，feature 模块共享）；本壳类仅为兼容保留。
 *             为保持旧调用方 {@code catch (spill.SandboxViolationException)} 语义，
 *             壳类把 core 异常包装为本包子类型再抛出（新代码直接 catch core 父类型亦可，
 *             本异常是其子类）。
 */
@Deprecated(forRemoval = false)
public class FileSandbox extends io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox {

    public FileSandbox(Path root, List<Path> additionalAllowedRoots) {
        super(root, additionalAllowedRoots);
    }

    @Override
    public Path resolve(String raw) {
        try {
            return super.resolve(raw);
        } catch (io.github.chyuan_cuihongyuan.buzhou.core.fs.SandboxViolationException e) {
            throw new SandboxViolationException(e.getMessage());
        }
    }

    @Override
    public Path resolveForWrite(String raw) {
        try {
            return super.resolveForWrite(raw);
        } catch (io.github.chyuan_cuihongyuan.buzhou.core.fs.SandboxViolationException e) {
            throw new SandboxViolationException(e.getMessage());
        }
    }
}
