package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Redis 大值（BIGKEY）审计读数（spec 801 / T1103，Redis BIGKEY 治理思想）：
 * 对<b>采样得到的</b> (键 → 序列化字节数) 做分族归类 + 阈值定级 + Top-N 排名
 * + 按族字节聚合——「Redis 为什么膨胀/哪个命名空间在吃内存」结构化。
 *
 * <p><b>纯函数</b>（确定性、与连接无关）：采样（SCAN + STRLEN/LLEN/HLEN 折算）
 * 是调用方/运维侧职责——本类只做归类、定级、排名的判定脑（同 705 布局审计
 * 「证据面不改行为」口径）。定级：WARN ≥ 阈值、CRIT ≥ 2×阈值（阿里云 BIGKEY
 * 惯例的简化档位）；Top 封顶 {@value #TOP_LIMIT}（有界纪律）。
 */
public final class RedisValueSizeAudit {

    /** Top 明细封顶。 */
    public static final int TOP_LIMIT = 32;

    /** 单条大值发现（severity ∈ WARN / CRIT）。 */
    public record Finding(String key, String family, long bytes, String severity, String hint) {
    }

    /** 按族聚合行（字节降序）。 */
    public record FamilyTotal(String family, long bytes, int keys, int overThreshold) {
    }

    /** 不可变审计报告。 */
    public record Report(List<Finding> top, List<FamilyTotal> families, int sampledKeys,
                         long sampledBytes, int warnBytes) {
    }

    private RedisValueSizeAudit() {
    }

    /**
     * 键族归类（前缀判定，{@code prefix:<rest>} 形状；semvec 为独立二级段）。
     * 未识别前缀归 {@code other}——不猜测。
     */
    public static String familyOf(String key) {
        if (key == null || key.isBlank()) {
            return "other";
        }
        String body = key.startsWith("buzhou:") ? key.substring("buzhou:".length()) : key;
        if (body.startsWith("semvec:")) {
            return "semvec";
        }
        int colon = body.indexOf(':');
        String head = colon > 0 ? body.substring(0, colon) : body;
        return switch (head) {
            case "idx", "msg", "msgid", "sum", "state", "statekeys", "lease", "obs" -> head;
            default -> "other";
        };
    }

    /** 族的治理提示（大值常见成因——读数侧人话）。 */
    private static String hintFor(String family) {
        return switch (family) {
            case "msg" -> "单条消息正文过大（附件/长文）——查 Spill 外置与 LongContentParam 阈值";
            case "sum" -> "摘要正文过大——查微压缩档位与九段摘要策略";
            case "state" -> "会话状态值过大——查 StateTtlCoverage 永生键与生产者归因";
            case "obs" -> "观测 span/event 过大——查事件 payload 审计与 TTL 逐出";
            case "lease" -> "租约键异常大——HASH 字段疑似污染，查 isSafeSessionId 摄入守卫";
            case "idx" -> "索引行过大——SessionInfo 异常膨胀";
            case "msgid" -> "消息 id 索引过大——findById 索引与正文应同寿";
            case "statekeys" -> "状态键集合过大——单会话状态键数失控";
            case "semvec" -> "向量桶过大——查语义缓存容量上限与权重预算驱逐";
            default -> "未识别命名空间大值——先归类再治理";
        };
    }

    /**
     * 大值审计：severity WARN ≥ warnBytes、CRIT ≥ 2×warnBytes；
     * top 按字节降序封顶 {@value #TOP_LIMIT}；families 按族字节降序；
     * 输入即事实（不做门槛过滤——低于阈值的键也计入族聚合）。
     */
    public static Report audit(Map<String, Long> sampledSizes, int warnBytes) {
        Objects.requireNonNull(sampledSizes, "sampledSizes");
        if (warnBytes < 1) {
            throw new IllegalArgumentException("warnBytes 必须 >= 1，实际 " + warnBytes);
        }
        int critBytes = warnBytes * 2;
        List<Finding> over = new ArrayList<>();
        Map<String, long[]> familyAgg = new LinkedHashMap<>(); // bytes, keys, overThreshold
        long totalBytes = 0;
        for (Map.Entry<String, Long> e : sampledSizes.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || e.getValue() < 0) {
                continue; // 脏采样跳过（诚实口径：不计入也不报错）
            }
            long bytes = e.getValue();
            totalBytes += bytes;
            String family = familyOf(e.getKey());
            long[] agg = familyAgg.computeIfAbsent(family, k -> new long[3]);
            agg[0] += bytes;
            agg[1]++;
            String severity = null;
            if (bytes >= critBytes) {
                severity = "CRIT";
            } else if (bytes >= warnBytes) {
                severity = "WARN";
            }
            if (severity != null) {
                agg[2]++;
                over.add(new Finding(e.getKey(), family, bytes, severity, hintFor(family)));
            }
        }
        over.sort(Comparator.comparingLong(Finding::bytes).reversed());
        List<Finding> top = over.size() > TOP_LIMIT ? List.copyOf(over.subList(0, TOP_LIMIT)) : List.copyOf(over);

        List<FamilyTotal> families = new ArrayList<>();
        familyAgg.forEach((family, agg) -> families.add(
                new FamilyTotal(family, agg[0], (int) agg[1], (int) agg[2])));
        families.sort(Comparator.comparingLong(FamilyTotal::bytes).reversed());

        return new Report(top, List.copyOf(families), sampledSizes.size(), totalBytes, warnBytes);
    }
}
