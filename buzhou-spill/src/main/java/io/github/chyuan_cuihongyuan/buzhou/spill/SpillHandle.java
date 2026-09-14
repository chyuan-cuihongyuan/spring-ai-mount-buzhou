package io.github.chyuan_cuihongyuan.buzhou.spill;
/** spec 1529 / T2309：溢写句柄——单次溢写的 uri/预览/回读元信息（会话内持有）。 */

public record SpillHandle(SpillUri uri, long sizeChars, String preview) {
}
