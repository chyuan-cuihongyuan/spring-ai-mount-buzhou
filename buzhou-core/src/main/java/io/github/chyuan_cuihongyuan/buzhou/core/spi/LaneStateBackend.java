package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * 泳道许可共享后端（spec 316 / T623，Redisson 分布式信号量语义收窄）：
 * {@code ToolLaneRegistry} 泳道的跨实例计数面——多实例共享一套许可
 * （慢工具道在实例间互斥）。泳道名与容量声明仍进程内本地。
 *
 * <p>诚实边界：进程崩溃未 release 的许可常驻——{@link #reset(String)} 运维
 * 清零（租约/TTL 自动回收复杂度不成比例，入档 spec 316）。
 *
 * @since 1.0.0
 */
public interface LaneStateBackend {

    /**
     * 原子取一个许可：当前已取 +1 ≤ permits 才提交（check-then-incr 一体）。
     *
     * @param lane     泳道名
     * @param permits 泳道容量（调用方本地声明）
     * @return 是否取得（false = 满道或后端不可达——fail-closed）
     */
    boolean tryAcquire(String lane, int permits);

    /** 归还一个许可（DECR floor-0——多还不欠）。 */
    void release(String lane);

    /** 运维清零（崩溃泄漏回收）。 */
    void reset(String lane);
}
