package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 对冲请求节省读面（L 会话 1700 系 R45 = effort #1744 / spec 1744 /
 * 票 T2689 + T2690 / impl 1344）——Google「The Tail at Scale」/ Envoy
 * request hedging 思想：{@link HedgedChatModel} 发对冲请求（主请求慢时
 * 再发一路），谁先回、省了多少尾延迟须有账——对冲赢率低 = 白花钱，
 * 赢率高 = 阈值可以更激进。
 *
 * <p>实例面线程安全：recordHedge()/recordPrimaryWin()/recordHedgeWin()/
 * recordLatencySaved(millis) 四计数+census（对冲赢率 −1 哨兵）。
 * 纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class HedgeStats {

    private final AtomicLong hedges = new AtomicLong();
    private final AtomicLong primaryWins = new AtomicLong();
    private final AtomicLong hedgeWins = new AtomicLong();
    private final AtomicLong latencySavedMillis = new AtomicLong();

    /** 记一次对冲请求发出。 */
    public void recordHedge() {
        hedges.incrementAndGet();
    }

    /** 记一次主请求先回（对冲白发）。 */
    public void recordPrimaryWin() {
        primaryWins.incrementAndGet();
    }

    /** 记一次对冲先回（对冲赚到）。 */
    public void recordHedgeWin() {
        hedgeWins.incrementAndGet();
    }

    /** 累计对冲节省时延（毫秒；负值忽略）。 */
    public void recordLatencySaved(long millis) {
        if (millis > 0) {
            latencySavedMillis.addAndGet(millis);
        }
    }

    /**
     * @param hedges             对冲发出数
     * @param primaryWins        主请求先回数
     * @param hedgeWins          对冲先回数
     * @param latencySavedMillis 累计节省时延
     * @param hedgeWinRatio      对冲赢率 hedgeWins/(primary+hedge)；无决胜哨兵 −1
     */
    public record HedgeCensus(long hedges, long primaryWins, long hedgeWins,
                              long latencySavedMillis, double hedgeWinRatio) {
    }

    /** 快照。 */
    public HedgeCensus census() {
        long decided = primaryWins.get() + hedgeWins.get();
        double ratio = decided == 0 ? -1d : (double) hedgeWins.get() / decided;
        return new HedgeCensus(hedges.get(), primaryWins.get(), hedgeWins.get(),
                latencySavedMillis.get(), ratio);
    }
}
