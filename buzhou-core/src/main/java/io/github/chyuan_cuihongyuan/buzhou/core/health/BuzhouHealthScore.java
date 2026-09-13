package io.github.chyuan_cuihongyuan.buzhou.core.health;

import java.util.ArrayList;
import java.util.List;

/**
 * impl-658 / spec 905：机制健康聚合评分（K8s probe aggregate / HTTP health
 * score 面板直觉——16+ 机制探针聚合成单一分数+分档，dashboard 一格、告警一条）。
 *
 * <p>评分口径与 {@link BuzhouHealth} 严格 DOWN 语义对齐：UP=100、UNKNOWN=50
 * （UNKNOWN=未启用非故障——中性 50 不奖不罚，避免误报也避免掩盖）、DOWN=0
 * （「无法履行核心职能」才 DOWN——重罚合理）。总分 = 算术平均取整。
 *
 * <p>纯函数：不触 store、不改 {@code BuzhouHealthEndpoint}（端点接线留装配轮）。
 */
public final class BuzhouHealthScore {

    /** UP 得分。 */
    public static final int SCORE_UP = 100;
    /** UNKNOWN 得分（未启用中性）。 */
    public static final int SCORE_UNKNOWN = 50;
    /** DOWN 得分。 */
    public static final int SCORE_DOWN = 0;
    /** healthy 分档下限（含）。 */
    public static final int HEALTHY_FLOOR = 90;
    /** degraded 分档下限（含；低于此为 unhealthy）。 */
    public static final int DEGRADED_FLOOR = 70;

    private static final String TIER_HEALTHY = "healthy";
    private static final String TIER_DEGRADED = "degraded";
    private static final String TIER_UNHEALTHY = "unhealthy";

    /**
     * 评分报告：{@code downMechanisms} 仅列 DOWN 机制（有界详情纪律——DOWN
     * 通常少数且是排障入口）；空机制清单约定 100/healthy（无机制=无故障）。
     */
    public record ScoreReport(int score, String tier, int upCount, int downCount,
                              int unknownCount, List<String> downMechanisms) {
    }

    private BuzhouHealthScore() {
    }

    /** 聚合评分（{@code contributors} 非空要求；null 项 fail-fast）。 */
    public static ScoreReport compute(List<BuzhouHealth> contributors) {
        if (contributors == null) {
            throw new IllegalArgumentException("contributors 必须非空（空清单传 List.of()）");
        }
        int up = 0;
        int down = 0;
        int unknown = 0;
        long total = 0;
        List<String> downMechanisms = new ArrayList<>();
        for (BuzhouHealth contributor : contributors) {
            if (contributor == null) {
                throw new IllegalArgumentException("contributors 含 null 项");
            }
            int s = switch (contributor.status()) {
                case UP -> {
                    up++;
                    yield SCORE_UP;
                }
                case UNKNOWN -> {
                    unknown++;
                    yield SCORE_UNKNOWN;
                }
                case DOWN -> {
                    down++;
                    downMechanisms.add(contributor.mechanism());
                    yield SCORE_DOWN;
                }
            };
            total += s;
        }
        int score = contributors.isEmpty()
                ? SCORE_UP
                : (int) Math.round((double) total / contributors.size());
        return new ScoreReport(score, tierOf(score), up, down, unknown,
                List.copyOf(downMechanisms));
    }

    private static String tierOf(int score) {
        if (score >= HEALTHY_FLOOR) {
            return TIER_HEALTHY;
        }
        return score >= DEGRADED_FLOOR ? TIER_DEGRADED : TIER_UNHEALTHY;
    }
}
