package io.github.chyuan_cuihongyuan.buzhou.core.exec;

/**
 * 固定间隔下次触发（spec 1870 / T2941 / impl 1471）——crontab/systemd
 * timer 的固定间隔网格语义：触发点钉在 epochStart + k×interval 的网格
 * 上——停机再启动不重排网格（错过就错过，补账不重贴）；nextFire 取
 * ≥ now 的最近网格点（含上——恰在格点即「现在到期」）。错过触发数
 *（missedFires）让补账显式：补几次、漏了多久的节奏一目了然。
 *
 * <p>纯函数零状态、确定性；只算不触发（调度归宿主）。
 */
public final class NextFireSchedule {

    private NextFireSchedule() {
    }

    /**
     * 下次触发：≥ now 的最近网格点（epochStart + k×interval，k ≥ 0；
     * now ≤ epochStart 时即 epochStart——首触发）。契约：interval ≥ 1、
     * 三时点 ≥ 0（fail-fast）。
     */
    public static long nextFireMillis(long intervalMillis, long epochStartMillis,
                                      long nowMillis) {
        validate(intervalMillis, epochStartMillis, nowMillis);
        if (nowMillis <= epochStartMillis) {
            return epochStartMillis;
        }
        long elapsed = nowMillis - epochStartMillis;
        long k = (elapsed + intervalMillis - 1) / intervalMillis; // ceil
        return epochStartMillis + k * intervalMillis;
    }

    /**
     * 错过触发数：(lastAcked, now] 内的网格点数——上次确认后漏了几次。
     * 契约：lastAcked 在网格上或为 epochStart 前哨（0 表示从首格起全算）；
     * lastAcked ≤ now。
     */
    public static long missedFires(long intervalMillis, long epochStartMillis,
                                   long lastAckedMillis, long nowMillis) {
        validate(intervalMillis, epochStartMillis, nowMillis);
        if (lastAckedMillis < 0 || lastAckedMillis > nowMillis) {
            throw new IllegalArgumentException(String.format(
                    "非法上次确认：%d（须 ≥ 0 且 ≤ now=%d）", lastAckedMillis,
                    nowMillis));
        }
        long first = firstGridAfter(intervalMillis, epochStartMillis, lastAckedMillis);
        long last = epochStartMillis
                + ((nowMillis - epochStartMillis) / intervalMillis) * intervalMillis;
        if (first > last || nowMillis < epochStartMillis) {
            return 0;
        }
        return (last - first) / intervalMillis + 1;
    }

    /** after 之后的第一个网格点（after 在 epoch 前即首格 epoch）。 */
    private static long firstGridAfter(long intervalMillis, long epochStartMillis,
                                       long after) {
        if (after < epochStartMillis) {
            return epochStartMillis;
        }
        long elapsed = after - epochStartMillis;
        long k = elapsed / intervalMillis + 1;
        return epochStartMillis + k * intervalMillis;
    }

    private static void validate(long intervalMillis, long epochStartMillis,
                                 long nowMillis) {
        if (intervalMillis < 1) {
            throw new IllegalArgumentException("intervalMillis 不能小于 1：" + intervalMillis);
        }
        if (epochStartMillis < 0 || nowMillis < 0) {
            throw new IllegalArgumentException(String.format(
                    "时点不能为负：epochStart=%d, now=%d", epochStartMillis, nowMillis));
        }
    }
}
