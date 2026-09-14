package io.github.chyuan_cuihongyuan.buzhou.core.spi;
/**
 * 租约获取结果——acquired 是否成功 + fencingToken 递增护栏令牌（提交点 fence 用）。
 */
public record LeaseAcquireResult(boolean acquired, long fencingToken) {
}
