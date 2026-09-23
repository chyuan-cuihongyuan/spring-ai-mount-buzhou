package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * fail2ban 封禁递升（spec 4046 / T6093 / impl 2147）——
 * maxretry/findtime/bantime.increment 思想（fail2ban 惯犯
 * 递升）：findTime 滑窗内失败 ≥ maxRetry 即禁（陈账按窗过期
 * 自然洗白——无窗全量计数的病解）；禁期时长 =
 * base × multiplier^(banCount−1) 封顶 maxBanTime——惯犯成本
 * 递增；禁期内再失败不计数不复禁（封锁即足够）；解禁后失败
 * 清零重计；禁即清失败窗（fail2ban 同语义）。
 *
 * <p>时钟注入：时间由调用方传入（确定性可回放）。与
 * RetryStormSentinel 互补：出向重试风暴 vs 入向滥用禁递升。
 */
public final class BanEscalation {

    private final int maxRetry;
    private final long findTime;
    private final long baseBanTime;
    private final double multiplier;
    private final long maxBanTime;
    private final Map<String, Deque<Long>> failures = new HashMap<>();
    private final Map<String, Long> bannedUntil = new HashMap<>();
    private final Map<String, Integer> banCounts = new HashMap<>();

    /** 定构（maxRetry≥1、findTime/baseBan>0、multiplier≥1、maxBan≥base 否则 fail-fast）。 */
    public BanEscalation(int maxRetry, long findTime, long baseBanTime,
            double multiplier, long maxBanTime) {
        if (maxRetry < 1 || findTime <= 0 || baseBanTime <= 0
                || multiplier < 1 || maxBanTime < baseBanTime) {
            throw new IllegalArgumentException("maxRetry≥1 / findTime>0 / baseBan>0 /"
                    + " multiplier≥1 / maxBan≥base：" + maxRetry + "/" + findTime + "/"
                    + baseBanTime + "/" + multiplier + "/" + maxBanTime);
        }
        this.maxRetry = maxRetry;
        this.findTime = findTime;
        this.baseBanTime = baseBanTime;
        this.multiplier = multiplier;
        this.maxBanTime = maxBanTime;
    }

    /**
     * 记账一次失败；窗满且未在禁即判禁。
     *
     * @param offender 滥用者标识
     * @param now 当前时刻（调用方时钟）
     * @return 裁决
     */
    public Verdict recordFailure(String offender, long now) {
        if (offender == null || offender.isEmpty()) {
            throw new IllegalArgumentException("offender 非空");
        }
        Long active = bannedUntil.get(offender);
        if (active != null && now < active) {
            return Verdict.banned(active, banCounts.get(offender), 0);   // 封锁即足够——不计数
        }
        Deque<Long> window = failures.computeIfAbsent(offender, key -> new ArrayDeque<>());
        while (!window.isEmpty() && window.peekFirst() <= now - findTime) {
            window.pollFirst();   // 滑窗——陈账过期洗白
        }
        window.addLast(now);
        if (window.size() < maxRetry) {
            return Verdict.observed(banCounts.getOrDefault(offender, 0), window.size());
        }
        int banCount = banCounts.merge(offender, 1, Integer::sum);
        long duration = escalate(banCount);
        long until = now + duration;
        bannedUntil.put(offender, until);
        window.clear();   // 禁即清失败窗（fail2ban 同语义）
        return Verdict.banned(until, banCount, duration);
    }

    /** 是否禁中（禁期自然到期）。 */
    public boolean isBanned(String offender, long now) {
        Long until = bannedUntil.get(offender);
        return until != null && now < until;
    }

    /** 前科次数读数。 */
    public int banCountOf(String offender) {
        return banCounts.getOrDefault(offender, 0);
    }

    /** 递升禁期：base × multiplier^(count−1) 封顶 maxBanTime。 */
    private long escalate(int banCount) {
        double duration = baseBanTime * Math.pow(multiplier, banCount - 1);
        return (long) Math.min(duration, maxBanTime);
    }

    /**
     * 失败记账裁决。
     *
     * @param banned 本次是否触发禁
     * @param banUntil 禁期截止（banned 时有意义）
     * @param banCount 前科次数（含本次若触发）
     * @param banDuration 本次禁期时长（banned 时有意义）
     * @param windowSize 当前滑窗失败数（banned 时为 0——禁即清窗）
     */
    public record Verdict(boolean banned, long banUntil, int banCount, long banDuration,
            int windowSize) {

        static Verdict banned(long until, int banCount, long duration) {
            return new Verdict(true, until, banCount, duration, 0);
        }

        static Verdict observed(int banCount, int windowSize) {
            return new Verdict(false, 0, banCount, 0, windowSize);
        }
    }
}
