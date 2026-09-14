package io.github.chyuan_cuihongyuan.buzhou.core.spi;
/**
 * 上下文窗口解析 SPI——按模型解析上下文窗口容量（记忆压缩 / spill 决策输入）。
 */
public interface ContextWindowResolver {

    int resolveWindow(String modelName);
}
