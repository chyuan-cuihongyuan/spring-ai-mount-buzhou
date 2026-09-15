package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

/**
 * Stale-While-Revalidate 策略（spec 1824 / T2849 / impl 1425）——HTTP
 * Cache-Control `max-age` + `stale-while-revalidate` / CDN 思想：缓存年龄
 * 分**新鲜窗**（直接服务）与**陈旧窗**（仍回旧值，同时异步后台刷新——
 * 用户拿到 5ms 旧数据而不是 500ms 阻塞新数据）两段；陈旧窗耗尽才真正
 * 过期（同步取）。读面自带「该异步刷新了吗」的一跳判定，尾延迟换轻微
 * 陈旧性的显式交易。
 *
 * <p>纯函数零状态、只判态不执行（刷新动作归宿主）。
 */
public final class StaleWhileRevalidatePolicy {

    private StaleWhileRevalidatePolicy() {
    }

    /** 服务路径三态：FRESH 直接服务 / STALE 旧值+异步刷新 / EXPIRED 同步取。 */
    public enum Serving {

        /** 新鲜窗内——直接服务缓存。 */
        FRESH,

        /** 陈旧窗内——回旧值并触发异步刷新（不阻塞调用方）。 */
        STALE,

        /** 陈旧窗耗尽——同步回源取新。 */
        EXPIRED
    }

    /**
     * 判态入口。契约：age/freshMillis/staleWindowMillis ≥ 0（fail-fast，
     * NaN 拒绝）；语义：age &lt; fresh → FRESH；age &lt; fresh + staleWindow
     * → STALE；否则 EXPIRED（边界归属：达 fresh 即进陈旧、达总寿即过期）。
     */
    public static Serving serving(long ageMillis, long freshMillis, long staleWindowMillis) {
        if (ageMillis < 0 || freshMillis < 0 || staleWindowMillis < 0) {
            throw new IllegalArgumentException(String.format(
                    "入参不能为负：age=%d, fresh=%d, staleWindow=%d",
                    ageMillis, freshMillis, staleWindowMillis));
        }
        if (ageMillis < freshMillis) {
            return Serving.FRESH;
        }
        if (ageMillis < freshMillis + staleWindowMillis) {
            return Serving.STALE;
        }
        return Serving.EXPIRED;
    }

    /**
     * 陈旧度读数：0=新鲜（钳零不外泄负值）、(0,1)=陈旧窗内进度、≥1=过期。
     * fresh=0 时 -1 哨兵（无新鲜窗语义不成立）。
     */
    public static double staleness(long ageMillis, long freshMillis, long staleWindowMillis) {
        serving(0, freshMillis, staleWindowMillis); // 契约复用校验
        if (freshMillis == 0) {
            return -1d;
        }
        return Math.max(0d, (double) (ageMillis - freshMillis) / staleWindowMillis);
    }
}
