package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能加载延迟读数（spec 832 / T1165，LangSmith 延迟分析扩散——839 使用
 * 计数的延迟姊妹面）：per-skill 最近 N 次加载耗时环 + p50/p95 + 最慢排行——
 * 「技能目录为什么越用越慢」（DB 技能正文膨胀/资源解析慢）从感觉变样本。
 *
 * <p>纯读数：per-skill 环（容量 {@value #RING}）+技能封顶 {@value #MAX_SKILLS}
 * （超限记 {@link #OVERFLOW} 桶——与 {@link SkillUsageStats} 同款口径）；
 * record(null/空白/负值) 忽略。喂点=LoadSkillTool 装配侧。
 */
public final class SkillLoadLatency {

    /** 单技能样本环容量。 */
    public static final int RING = 32;
    /** 技能数封顶。 */
    public static final int MAX_SKILLS = 1024;
    /** 溢出桶名（与 SkillUsageStats 同款）。 */
    public static final String OVERFLOW = "__overflow__";

    /** 单技能延迟行。 */
    public record SkillLatency(String skill, long loads, long p50Millis, long p95Millis, long maxMillis) {
    }

    private static final class Ring {
        final long[] buffer = new long[RING];
        int size;
        int head;
        long total;
        long max;
    }

    private final Map<String, Ring> rings = new ConcurrentHashMap<>();

    /** 记录一次加载耗时（毫秒；null/空白/负值忽略；超封顶记溢出桶）。 */
    public void record(String skill, long millis) {
        if (skill == null || skill.isBlank() || millis < 0) {
            return;
        }
        String key = skill;
        if (!rings.containsKey(key) && rings.size() >= MAX_SKILLS) {
            key = OVERFLOW; // 超封顶并入溢出桶（SkillUsageStats 同款口径）
        }
        Ring ring = rings.computeIfAbsent(key, k -> new Ring());
        synchronized (ring) {
            if (ring.size < RING) {
                ring.buffer[(ring.head + ring.size) % RING] = millis;
                ring.size++;
            } else {
                ring.buffer[ring.head] = millis;
                ring.head = (ring.head + 1) % RING;
            }
            ring.total += millis;
            ring.max = Math.max(ring.max, millis);
        }
    }

    /** 单技能行（未知 null）。 */
    public SkillLatency stats(String skill) {
        Ring ring = skill == null ? null : rings.get(skill);
        if (ring == null) {
            return null;
        }
        List<Long> samples = new ArrayList<>(ring.size);
        synchronized (ring) {
            for (int i = 0; i < ring.size; i++) {
                samples.add(ring.buffer[(ring.head + i) % RING]);
            }
        }
        samples.sort(Long::compare);
        return new SkillLatency(skill, samples.size(),
                nearestRank(samples, 50), nearestRank(samples, 95), ring.max);
    }

    /** 全技能行（按 p95 降序——最慢在前）。 */
    public List<SkillLatency> slowest() {
        List<SkillLatency> all = new ArrayList<>();
        for (String skill : rings.keySet()) {
            SkillLatency s = stats(skill);
            if (s != null) {
                all.add(s);
            }
        }
        all.sort(Comparator.comparingLong(SkillLatency::p95Millis).reversed());
        return List.copyOf(all);
    }

    private static long nearestRank(List<Long> sorted, int p) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        return sorted.get(Math.min(Math.max(rank, 1), sorted.size()) - 1);
    }
}
