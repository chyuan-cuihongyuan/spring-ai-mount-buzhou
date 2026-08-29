package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 技能使用统计（spec 140 §A / T465，Backstage catalog score / Caffeine
 * hitRate 观测面借鉴）：per-skill 加载计数——「目录里什么在被用、什么在吃灰」
 * 的进程内事实表。load 成功点打点（{@link LoadSkillTool} 接线），排行与
 * 零使用清单供目录治理（下架/改文案/改绑定的证据面）。
 *
 * <p><b>口径</b>：有界 {@value #MAX_SKILLS} 封顶折 {@code __overflow__}
 * （既有技能继续细分，新技能名不再扩张——overflow 占位是封顶后唯一可新增键，
 * 与 ErrorSignatures 同款封顶语义；技能目录本身有界，此为防御面）；{@code reset()} 窗口清零（export → reset
 * 循环，spec 121 同纪律）；排序稳定（count 降序 + 名字典序）。
 */
public final class SkillUsageStats {

    /** 技能名封顶（防御面：目录本身有界）。 */
    public static final int MAX_SKILLS = 1024;
    /** 封顶后新技能折入的计数键。 */
    public static final String OVERFLOW = "__overflow__";

    /** 单技能使用快照（排行/治理面）。 */
    public record SkillUsage(String skill, long loads) {
    }

    private static final AtomicReference<SkillUsageStats> GLOBAL =
            new AtomicReference<>(new SkillUsageStats());

    private final Map<String, AtomicLong> loads = new ConcurrentHashMap<>();

    private SkillUsageStats() {
    }

    /** 独立实例（测试/宿主自管作用域）。 */
    public static SkillUsageStats create() {
        return new SkillUsageStats();
    }

    /** 全局默认实例（LoadSkillTool 打点用——ErrorSignatures 全局旋钮同模式）。 */
    public static SkillUsageStats global() {
        return GLOBAL.get();
    }

    /** 测试替换/清理（null = 换新；@AfterEach 纪律）。 */
    public static void install(SkillUsageStats stats) {
        GLOBAL.set(stats == null ? new SkillUsageStats() : stats);
    }

    /** 记一次成功加载（空白名拒绝——打点面不做静默吞）。 */
    public void recordLoad(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            throw new IllegalArgumentException("skillName must not be blank");
        }
        AtomicLong counter = loads.get(skillName);
        if (counter != null) {
            counter.incrementAndGet();
            return;
        }
        if (loads.size() >= MAX_SKILLS) {
            loads.computeIfAbsent(OVERFLOW, k -> new AtomicLong()).incrementAndGet();
            return;
        }
        loads.computeIfAbsent(skillName, k -> new AtomicLong()).incrementAndGet();
    }

    /** 使用排行 top-N（count 降序，同 count 名字典序——输出稳定）。 */
    public List<SkillUsage> topUsed(int n) {
        return loads.entrySet().stream()
                .sorted((a, b) -> {
                    int byCount = Long.compare(b.getValue().get(), a.getValue().get());
                    return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
                })
                .limit(Math.max(0, n))
                .map(e -> new SkillUsage(e.getKey(), e.getValue().get()))
                .toList();
    }

    /**
     * 零使用清单（目录治理证据面）：catalog 全名里从未加载过的技能，按名字典序
     * ——「在册但在吃灰」的候选下架/整改名单。
     */
    public List<String> unused(List<String> catalogSkillNames) {
        List<String> out = new ArrayList<>();
        for (String name : catalogSkillNames) {
            AtomicLong counter = loads.get(name);
            if (counter == null || counter.get() == 0) {
                out.add(name);
            }
        }
        return out.stream().sorted().toList();
    }

    /** 在册技能数（含 overflow 占位）。 */
    public int distinct() {
        return loads.size();
    }

    /** 窗口清零（export → reset 循环——每窗口一份排行）。 */
    public void reset() {
        loads.clear();
    }
}
