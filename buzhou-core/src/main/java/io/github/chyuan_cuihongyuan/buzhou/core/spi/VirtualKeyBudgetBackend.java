package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * 虚拟 key 配额共享后端（spec 315 / T621，Redisson 分布式限额器借鉴）：
 * {@code VirtualKeys} 的计数面委托点——多实例部署共享同一份额度（N 实例 ≠ N 倍烧钱）。
 * 限额声明（register）仍进程内本地——本 SPI 只共享「已用多少」的原子计数。
 *
 * @since 1.0.0
 */
public interface VirtualKeyBudgetBackend {

    /**
     * 原子扣减：已用 + tokens ≤ limitTokens 才提交（check-then-incr 一体原子——
     * 竞态窗口零）。
     *
     * @param key         虚拟 key
     * @param tokens      本次扣减（>0）
     * @param limitTokens 该 key 限额（调用方本地声明）
     * @return 是否成功（false = 越限或后端不可达——fail-closed：配额面宁可拒绝不可超支）
     */
    boolean trySpend(String key, long tokens, long limitTokens);

    /** 该 key 已用 tokens（未见 0）。 */
    long usedTokens(String key);

    /** 该 key 计数清零（窗口重置）。 */
    void reset(String key);
}
