package io.github.chyuan_cuihongyuan.buzhou.spill;

public record RangeReadResult(String content, long totalChars, boolean truncated, String nextCursor) {
}
