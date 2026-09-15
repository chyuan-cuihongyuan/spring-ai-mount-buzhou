package io.github.chyuan_cuihongyuan.buzhou.mcp;

import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP 重连退避实效读面（L 会话 1700 系 R37 = effort #1736 / spec 1736 /
 * 票 T2673 + T2674 / impl 1336）——gRPC channelz 的连接健康遥测思想：
 * MCP 服务端断线重连的退避节奏（maxDuration/avgDuration）与重连成功率
 * 须有账——退避越走越长且成功率不涨 = 服务端真挂了，别再无限试。
 * {@link McpConnectTelemetry} 管建连遥测，本面管重连周期实效。
 *
 * <p>实例面线程安全：recordAttempt(backoffMillis)（本次尝试用的退避值，
 * 负值忽略）/recordSuccess()/recordGiveUp() 三计数+census（成功率/
 * 最大退避/退避和）。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class McpReconnectStats {

    private final AtomicLong attempts = new AtomicLong();
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong giveUps = new AtomicLong();
    private final AtomicLong backoffSumMillis = new AtomicLong();
    private final AtomicLong maxBackoffMillis = new AtomicLong();

    /** 记一次重连尝试（backoff = 本次尝试前等待的退避毫秒）。 */
    public void recordAttempt(long backoffMillis) {
        if (backoffMillis < 0) {
            return;
        }
        attempts.incrementAndGet();
        backoffSumMillis.addAndGet(backoffMillis);
        maxBackoffMillis.getAndUpdate(prev -> Math.max(prev, backoffMillis));
    }

    /** 记一次重连成功。 */
    public void recordSuccess() {
        successes.incrementAndGet();
    }

    /** 记一次放弃（退避封顶/重试预算耗尽）。 */
    public void recordGiveUp() {
        giveUps.incrementAndGet();
    }

    /**
     * @param attempts          尝试数
     * @param successes         成功数
     * @param giveUps           放弆数
     * @param successRatio      成功率 successes/attempts；无尝试哨兵 −1
     * @param maxBackoffMillis  期间最大退避
     * @param avgBackoffMillis  平均退避（无尝试 −1）
     */
    public record ReconnectCensus(long attempts, long successes, long giveUps,
                                  double successRatio, long maxBackoffMillis,
                                  double avgBackoffMillis) {
    }

    /** 快照。 */
    public ReconnectCensus census() {
        long tries = attempts.get();
        double ratio = tries == 0 ? -1d : (double) successes.get() / tries;
        double avg = tries == 0 ? -1d : (double) backoffSumMillis.get() / tries;
        return new ReconnectCensus(tries, successes.get(), giveUps.get(),
                ratio, maxBackoffMillis.get(), avg);
    }
}
