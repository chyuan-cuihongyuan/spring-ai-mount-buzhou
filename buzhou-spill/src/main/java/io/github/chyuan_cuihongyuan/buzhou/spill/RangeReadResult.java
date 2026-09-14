package io.github.chyuan_cuihongyuan.buzhou.spill;
/** spec 1529 / T2309：区间回读结果——请求区间的内容切片与总长元信息。 */

public record RangeReadResult(String content, long totalChars, boolean truncated, String nextCursor) {
}
