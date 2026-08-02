package io.github.chyuan_cuihongyuan.buzhou.core.spi;

public record LeaseAcquireResult(boolean acquired, long fencingToken) {
}
