package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PII 命中统计（spec 144 §A / T469，合规报表面——Presidio anonymizer 统计
 * 口径借鉴）：内置类型 + 自定义规则名的命中计数进程内有界表——「哪类 PII
 * 最常出现在工具输出/用户输入」是脱敏策略调优与合规审计的共用事实。
 *
 * <p><b>口径</b>：内置 {@link PiiType} 天然有界（枚举）；自定义规则名封顶
 * {@value #MAX_CUSTOM_RULES} 折 {@code __overflow__}（新名不再扩张——与
 * ErrorSignatures 同款封顶语义）；{@code reset()} 窗口清零（export → reset
 * 循环，spec 121 同纪律）；排序稳定（count 降序 + 名字典序）。
 */
public final class PiiHitStats {

    /** 自定义规则名封顶（防御面——规则本身有命名校验）。 */
    public static final int MAX_CUSTOM_RULES = 64;
    /** 封顶后新规则名折入的计数键。 */
    public static final String OVERFLOW = "__overflow__";

    /** 单条命中统计（builtin 与 custom 统一名字面——报表行）。 */
    public record Hit(String name, long count) {
    }

    /**
     * 命中侧（spec 313 / T617，Presidio 统计分侧深化）：用户输入（预防提示面）vs
     * 工具输出（脱敏规则面）是两个策略面——分侧列拆开调优。UNSPECIFIED = 旧调用方
     * 单参 record 兜底（兼容未升级宿主）。
     */
    public enum Side { INPUT, OUTPUT, UNSPECIFIED }

    private static final AtomicReference<PiiHitStats> GLOBAL =
            new AtomicReference<>(new PiiHitStats());

    private final Map<String, AtomicLong> counts = new ConcurrentHashMap<>();
    private final Map<String, EnumMap<Side, AtomicLong>> sideCounts = new ConcurrentHashMap<>();

    private PiiHitStats() {
    }

    /** 独立实例（测试/宿主自管作用域）。 */
    public static PiiHitStats create() {
        return new PiiHitStats();
    }

    /** 全局默认实例（脱敏钩子打点用——ErrorSignatures 全局旋钮同模式）。 */
    public static PiiHitStats global() {
        return GLOBAL.get();
    }

    /** 测试替换/清理（null = 换新；@AfterEach 纪律）。 */
    public static void install(PiiHitStats stats) {
        GLOBAL.set(stats == null ? new PiiHitStats() : stats);
    }

    /** 记一次内置类型命中（side = UNSPECIFIED——旧调用方兼容）。 */
    public void record(PiiType type) {
        record(type, Side.UNSPECIFIED);
    }

    /** 记一次内置类型命中（分侧——spec 313）。 */
    public void record(PiiType type, Side side) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        counts.computeIfAbsent(type.name(), k -> new AtomicLong()).incrementAndGet();
        sideCount(type.name(), side).incrementAndGet();
    }

    /** 记一次自定义规则命中（封顶折 overflow；空白名拒绝；side = UNSPECIFIED）。 */
    public void recordCustom(String ruleName) {
        recordCustom(ruleName, Side.UNSPECIFIED);
    }

    /** 记一次自定义规则命中（分侧——封顶判定同总量口径）。 */
    public void recordCustom(String ruleName, Side side) {
        if (ruleName == null || ruleName.isBlank()) {
            throw new IllegalArgumentException("ruleName must not be blank");
        }
        AtomicLong counter = counts.get(ruleName);
        if (counter != null) {
            counter.incrementAndGet();
            sideCount(ruleName, side).incrementAndGet();
            return;
        }
        if (customNameCount() >= MAX_CUSTOM_RULES) {
            counts.computeIfAbsent(OVERFLOW, k -> new AtomicLong()).incrementAndGet();
            sideCount(OVERFLOW, side).incrementAndGet();
            return;
        }
        counts.computeIfAbsent(ruleName, k -> new AtomicLong()).incrementAndGet();
        sideCount(ruleName, side).incrementAndGet();
    }

    private AtomicLong sideCount(String name, Side side) {
        return sideCounts.computeIfAbsent(name, k -> new EnumMap<>(Side.class))
                .computeIfAbsent(side == null ? Side.UNSPECIFIED : side, k -> new AtomicLong());
    }

    /** 分侧排行 top-N（count 降序 + 名字典序——与 top() 同稳定序；spec 313）。 */
    public List<Hit> topBySide(Side side, int n) {
        return sideCounts.entrySet().stream()
                .filter(e -> e.getValue().containsKey(side))
                .sorted((a, b) -> {
                    int byCount = Long.compare(
                            b.getValue().get(side).get(), a.getValue().get(side).get());
                    return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
                })
                .limit(Math.max(0, n))
                .map(e -> new Hit(e.getKey(), e.getValue().get(side).get()))
                .toList();
    }

    /** 分侧点查（未见过 0——诚实空值）。 */
    public long countOf(String name, Side side) {
        EnumMap<Side, AtomicLong> bySide = sideCounts.get(name);
        AtomicLong counter = bySide == null ? null : bySide.get(side);
        return counter == null ? 0L : counter.get();
    }

    /** 命中排行 top-N（count 降序，同 count 名字典序——报表稳定序）。 */
    public List<Hit> top(int n) {
        return counts.entrySet().stream()
                .sorted((a, b) -> {
                    int byCount = Long.compare(b.getValue().get(), a.getValue().get());
                    return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
                })
                .limit(Math.max(0, n))
                .map(e -> new Hit(e.getKey(), e.getValue().get()))
                .toList();
    }

    /** 单名计数（未见过 0——诚实空值）。 */
    public long countOf(String name) {
        AtomicLong counter = counts.get(name);
        return counter == null ? 0L : counter.get();
    }

    /** 在册名数（含 overflow 占位）。 */
    public int distinct() {
        return counts.size();
    }

    /** 窗口清零（export → reset 循环——每窗口一份合规报表；分侧同清）。 */
    public void reset() {
        counts.clear();
        sideCounts.clear();
    }

    /** 自定义名计数（内置枚举名之外的键——封顶判定用）。 */
    private long customNameCount() {
        long custom = 0;
        for (String name : counts.keySet()) {
            if (!isBuiltIn(name)) {
                custom++;
            }
        }
        return custom;
    }

    private static boolean isBuiltIn(String name) {
        for (PiiType type : PiiType.values()) {
            if (type.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** 占位符中提取的自定义规则名列表中逐个记数（钩子接线便利面）。 */
    public void recordAll(List<String> customRuleNames) {
        if (customRuleNames != null) {
            customRuleNames.forEach(this::recordCustom);
        }
    }

    /**
     * 从脱敏产物提取自定义规则命中名（{@code [PII:NAME]} 占位符；内置类型名剔除
     * 防双计）——输入/输出双钩共用（spec 164 / T517）。
     */
    public static List<String> extractCustomRuleNames(String redacted) {
        List<String> names = new ArrayList<>();
        if (redacted == null || redacted.isEmpty()) {
            return names;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\[PII:([A-Z0-9_]{2,32})\\]").matcher(redacted);
        while (m.find()) {
            String name = m.group(1);
            boolean builtIn = false;
            for (PiiType type : PiiType.values()) {
                if (type.name().equals(name)) {
                    builtIn = true;
                    break;
                }
            }
            if (!builtIn) {
                names.add(name);
            }
        }
        return names;
    }
}
