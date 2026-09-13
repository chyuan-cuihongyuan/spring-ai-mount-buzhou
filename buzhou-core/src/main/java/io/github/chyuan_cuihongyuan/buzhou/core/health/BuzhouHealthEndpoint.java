package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 只读快照端点 {@code /actuator/buzhou}（impl-41 / spec 13 §T66）：聚合容器内全部
 * {@link BuzhouHealth} 机制贡献——运维一屏看全 buzhou 各机制状态与有界详情
 * （不写操作、无敏感字段：审计密钥/会话内容绝不出现）。
 */
@Endpoint(id = "buzhou")
public final class BuzhouHealthEndpoint {

    private final List<BuzhouHealth> contributors;

    public BuzhouHealthEndpoint(List<BuzhouHealth> contributors) {
        this.contributors = contributors == null ? List.of() : List.copyOf(contributors);
    }

    @ReadOperation
    public Map<String, Object> buzhouSnapshot() {
        Map<String, Object> mechanisms = new LinkedHashMap<>();
        for (BuzhouHealth contributor : contributors) {
            Map<String, Object> entry = new LinkedHashMap<>();
            // impl-670 / spec 917：mechanism/status 读取均隔离（此前仅 details 有
            // safe 壳——单机制爆炸会炸整个端点，e2e 实证后三处补齐）
            entry.put("status", safeStatus(contributor));
            entry.put("details", safeDetails(contributor));
            mechanisms.put(safeMechanism(contributor), entry);
        }
        // impl-670 / spec 917：聚合评分段（spec 905 装配留位兑现——dashboard 一格）
        return Map.of("mechanisms", mechanisms, "score", safeScore());
    }

    /** status 读取隔离（异常降级 DOWN + error 注记——宁报故障不静默）。 */
    private String safeStatus(BuzhouHealth contributor) {
        try {
            return contributor.status().name();
        } catch (RuntimeException e) {
            return "DOWN"; // 读取即炸 = 无法确认健康——从严按 DOWN（不静默吞）
        }
    }

    /** mechanism 读取隔离（异常降级占位名——键唯一性由类名兜底）。 */
    private String safeMechanism(BuzhouHealth contributor) {
        try {
            return contributor.mechanism();
        } catch (RuntimeException e) {
            return "unknown-mechanism@" + contributor.getClass().getName();
        }
    }

    /** impl-670 / spec 917：评分投影（compute 异常降级——单点故障不炸端点）。 */
    private Map<String, Object> safeScore() {
        try {
            BuzhouHealthScore.ScoreReport report = BuzhouHealthScore.compute(contributors);
            Map<String, Object> score = new LinkedHashMap<>();
            score.put("score", report.score());
            score.put("tier", report.tier());
            score.put("upCount", report.upCount());
            score.put("downCount", report.downCount());
            score.put("unknownCount", report.unknownCount());
            score.put("downMechanisms", report.downMechanisms());
            return score;
        } catch (RuntimeException e) {
            return Map.of("scoreError", String.valueOf(e.getMessage()));
        }
    }

    private Map<String, Object> safeDetails(BuzhouHealth contributor) {
        try {
            return contributor.details();
        } catch (RuntimeException e) {
            return Map.of("detailsError", String.valueOf(e.getMessage()));
        }
    }
}
