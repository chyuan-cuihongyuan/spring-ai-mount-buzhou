package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 阻塞期审计（spec 1890 / T2981 / impl 1491）——Oracle ASH 会话
 * 状态语义：任何时刻要么 ON_CPU 在算、要么 WAITING 在等（锁/I/O）。
 * 轮次/工具执行的耗时按构成分诊：「算得慢」加算力、「等得久」查
 * 锁或换盘——总时长一把尺没有分诊能力。
 *
 * <p>纯函数零状态；账面守恒（三类之和 = 总时长）fail-fast。
 */
public final class WaitProfileAudit {

    private WaitProfileAudit() {
    }

    /** 主导状态：ON_CPU / IO_WAIT / LOCK_WAIT（并列按此固定序）。 */
    public enum DominantClass { ON_CPU, IO_WAIT, LOCK_WAIT }

    /** 分诊画像：三类时长 + 总时长 + 阻塞比 + 主导状态。 */
    public record Profile(long onCpu, long lockWait, long ioWait, long elapsed,
                          double blockingRatio, DominantClass dominant) {
    }

    /**
     * 分诊画像：账面守恒 onCpu + lockWait + ioWait = elapsed（不等
     * fail-fast——时段覆盖必须完整）。
     */
    public static Profile profile(long onCpu, long lockWait, long ioWait,
                                  long elapsed) {
        if (onCpu < 0 || lockWait < 0 || ioWait < 0) {
            throw new IllegalArgumentException(String.format(
                    "时长不能为负：onCpu=%d, lockWait=%d, ioWait=%d",
                    onCpu, lockWait, ioWait));
        }
        if (elapsed < 0) {
            throw new IllegalArgumentException("elapsed 不能为负：" + elapsed);
        }
        if (onCpu + lockWait + ioWait != elapsed) {
            throw new IllegalArgumentException(String.format(
                    "账面不守恒：%d+%d+%d != %d", onCpu, lockWait, ioWait, elapsed));
        }
        double blocking = elapsed == 0 ? 0.0
                : (double) (lockWait + ioWait) / elapsed;
        return new Profile(onCpu, lockWait, ioWait, elapsed, blocking,
                dominantClass(onCpu, ioWait, lockWait));
    }

    /**
     * 主导状态：取大，并列按 ON_CPU > IO_WAIT > LOCK_WAIT 固定序
     * （确定性可回放）。
     */
    public static DominantClass dominantClass(long onCpu, long ioWait,
                                              long lockWait) {
        if (onCpu >= ioWait && onCpu >= lockWait) {
            return DominantClass.ON_CPU;
        }
        if (ioWait >= lockWait) {
            return DominantClass.IO_WAIT;
        }
        return DominantClass.LOCK_WAIT;
    }

    /**
     * 阻塞比：（锁等 + I/O 等）/ 总时长——「生命里用于等待的比例」。
     */
    public static double blockingRatio(long lockWait, long ioWait, long elapsed) {
        if (elapsed < 0) {
            throw new IllegalArgumentException("elapsed 不能为负：" + elapsed);
        }
        if (elapsed == 0) {
            return 0.0;
        }
        return (double) (lockWait + ioWait) / elapsed;
    }
}
