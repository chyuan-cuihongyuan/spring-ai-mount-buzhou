package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

/** 熔断器三态（spec 15「熔断器」）。 */
public enum CircuitState {
    CLOSED, OPEN, HALF_OPEN
}
