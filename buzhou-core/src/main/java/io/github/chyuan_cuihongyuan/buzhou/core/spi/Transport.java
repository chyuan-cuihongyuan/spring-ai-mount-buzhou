package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * MCP 传输方式（spec 04）。SSE 不提供：Spring AI 2.0.0 起 {@code @Deprecated(forRemoval=true)}。
 */
public enum Transport {
    STDIO,
    STREAMABLE_HTTP
}
