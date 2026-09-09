package io.github.chyuan_cuihongyuan.buzhou.guard.secret;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 秘密命中统计（spec 418 / T727，PiiHitStats 同构镜像——86/313 先例）：
 * 全局持有 + 三侧计数（INPUT 用户粘贴 / OUTBOUND 出站参数 / OUTPUT 工具
 * 结果——对应 SecretScanHook 三缝）。snapshot() 排序报表行（不清零——
 * 趋势对比用两次快照差）。
 */
public final class SecretHitStats {

    /** 命中侧（= hook 三缝）。 */
    public enum Side { INPUT, OUTBOUND, OUTPUT }

    /** 报表行（name = SecretType 名）。 */
    public record Hit(String name, Side side, long count) {
    }

    private static final AtomicReference<SecretHitStats> GLOBAL =
            new AtomicReference<>(new SecretHitStats());

    private final Map<String, EnumMap<Side, AtomicLong>> counts = new ConcurrentHashMap<>();

    private SecretHitStats() {
    }

    public static SecretHitStats global() {
        return GLOBAL.get();
    }

    /** 测试替换/清理（null = 换新）。 */
    public static void install(SecretHitStats stats) {
        GLOBAL.set(stats == null ? new SecretHitStats() : stats);
    }

    /** 记一次命中。 */
    public void record(SecretType type, Side side) {
        record(type.name(), side);
    }

    /** 记一次命中（名字面——与 PiiHitStats.recordCustom 口径对齐）。 */
    public void record(String name, Side side) {
        counts.computeIfAbsent(name == null ? "UNKNOWN" : name,
                k -> new EnumMap<>(Side.class))
                .computeIfAbsent(side == null ? Side.OUTPUT : side, k -> new AtomicLong())
                .incrementAndGet();
    }

    /** 快照（name 字典序；零命中类型不出现——只记发生过的）。 */
    public List<Hit> snapshot() {
        return counts.entrySet().stream()
                .flatMap(e -> e.getValue().entrySet().stream()
                        .map(s -> new Hit(e.getKey(), s.getKey(), s.getValue().get())))
                .sorted(Comparator.comparing(Hit::name).thenComparing(Hit::side))
                .toList();
    }
}
