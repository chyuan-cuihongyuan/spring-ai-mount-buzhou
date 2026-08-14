package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 装配占位 bean（impl-41 / spec 13 §T66）：构造副作用 = 把 micrometer 实现装进
 * {@link BuzhouMetricsHolder} 全局单点——无 micrometer 时不创建本 bean（holder 保持
 * no-op）。生命周期随容器（容器销毁后 holder 引用失效于下一个上下文装配时覆盖）。
 */
public final class BuzhouMetricsHolderInstaller {

    public BuzhouMetricsHolderInstaller() {
    }
}
