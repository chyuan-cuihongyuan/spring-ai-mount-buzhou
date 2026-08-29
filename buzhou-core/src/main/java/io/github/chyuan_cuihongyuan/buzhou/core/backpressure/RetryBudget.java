package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 重试预算（spec 178 §A / T534，Twitter Finagle retry budget 借鉴）：重试
 * 配额按「请求流量的百分比」持续累积——每请求存入 {@code percent}%（毫单位
 * 累计），每次重试支取 1；余额不足即拒绝重试。<b>防重试风暴</b>：上游故障时
 * 重试量自动被压到流量占比内，故障恢复后配额自动回填，无需窗口重置。
 *
 * <p><b>口径</b>：初始底数 {@code minBalance}（冷启动期也允许少量重试——
 * 零底数会让首批失败全裸）；连续累积无窗口（Finagle 同款——「预算随流量
 * 自适应」正是要义）；拒绝计数 {@code denied()} 可观测。毫单位内部整数
 * （percent=20 → 每请求 200 毫单位；1000 毫单位 = 1 次重试）。
 */
public final class RetryBudget {

    private final long minBalanceMillis;
    private final long depositMillisPerCall;
    private final AtomicLong balanceMillis;
    private final AtomicLong withdrawn;
    private final AtomicLong denied;

    private RetryBudget(double percentPerCall, long minBalance) {
        this.depositMillisPerCall = Math.round(percentPerCall * 10); // 20% → 200 毫单位
        this.minBalanceMillis = minBalance * 1_000;
        this.balanceMillis = new AtomicLong(this.minBalanceMillis);
        this.withdrawn = new AtomicLong();
        this.denied = new AtomicLong();
    }

    /**
     * @param percentPerCall 每请求存入百分比 (0, 1000]
     * @param minBalance     初始/下限重试次数（≥0；0 = 冷启动零重试裸奔——慎用）
     */
    public static RetryBudget of(double percentPerCall, long minBalance) {
        if (percentPerCall <= 0 || percentPerCall > 1000 || minBalance < 0) {
            throw new IllegalArgumentException(
                    "percentPerCall in (0,1000] and minBalance >= 0 required: "
                            + percentPerCall + ", " + minBalance);
        }
        return new RetryBudget(percentPerCall, minBalance);
    }

    /** 每次请求存入（正常调用与失败调用都存——流量就是预算来源）。 */
    public void deposit() {
        balanceMillis.addAndGet(depositMillisPerCall);
    }

    /** 批量存入（spec 206 §A / T570：n 次调用一次记账——批量路径免循环；n &lt; 0 拒绝）。 */
    public void deposit(int calls) {
        if (calls < 0) {
            throw new IllegalArgumentException("calls must be >= 0: " + calls);
        }
        balanceMillis.addAndGet(depositMillisPerCall * (long) calls);
    }

    /** 支取一次重试（余额不足 false 且计数——风暴被压制的证据面）。 */
    public boolean tryAcquire() {
        while (true) {
            long current = balanceMillis.get();
            if (current < 1_000) {
                denied.incrementAndGet();
                return false;
            }
            if (balanceMillis.compareAndSet(current, current - 1_000)) {
                withdrawn.incrementAndGet();
                return true;
            }
        }
    }

    /** 当前余额（整数次；毫单位截断——健康/测试面）。 */
    public long balance() {
        return balanceMillis.get() / 1_000;
    }

    /** 累计放行重试数。 */
    public long withdrawn() {
        return withdrawn.get();
    }

    /** 累计拒绝重试数（风暴被压制的证据面）。 */
    public long denied() {
        return denied.get();
    }

    /** 回填到底数（运维逃逸面——故障演练后手动补额）。 */
    public void refill() {
        balanceMillis.set(minBalanceMillis);
    }
}
