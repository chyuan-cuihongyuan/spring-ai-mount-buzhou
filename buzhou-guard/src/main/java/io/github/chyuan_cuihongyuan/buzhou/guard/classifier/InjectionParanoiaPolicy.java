package io.github.chyuan_cuihongyuan.buzhou.guard.classifier;

import java.util.Objects;

/**
 * 注入检测分级策略（spec 826 / T1153，ModSecurity paranoia levels 借鉴——
 * 检测敏感度分 1-4 级，级高则规则更激进、误报更多但更安全）：把
 * {@link InjectionClassifier.Verdict} 的连续分数按 paranoia 级映射为三态
 * 裁决——BLOCK（≥ 阈值）/ LOG（阈值下方 0.1 观察带）/ ALLOW。
 *
 * <p>阈值表（标准档，类常量可查）：L1=0.95（只拦几乎确定）、L2=0.85、
 * L3=0.70、L4=0.50（激进——高误报容忍场景如红队演练）。纯函数——不改变
 * 任何 classifier 行为（裁决采用与否归调用方）。
 */
public final class InjectionParanoiaPolicy {

    /** paranoia 级别（1 最保守 → 4 最激进）。 */
    public enum Level {
        L1(0.95), L2(0.85), L3(0.70), L4(0.50);

        private final double threshold;

        Level(double threshold) {
            this.threshold = threshold;
        }

        /** 该级的 BLOCK 阈值。 */
        public double threshold() {
            return threshold;
        }
    }

    /** 裁决三态。 */
    public enum Action { BLOCK, LOG, ALLOW }

    /** 观察带宽度（阈值下方仍记 LOG 的分数区间）。 */
    public static final double OBSERVATION_BAND = 0.10;

    /** 单条裁决（不可变）。 */
    public record Decision(Level level, double score, Action action) {
    }

    private InjectionParanoiaPolicy() {
    }

    /** 按级裁决：score ≥ 阈值 → BLOCK；≥ 阈值−观察带 → LOG；否则 ALLOW。 */
    public static Decision decide(InjectionClassifier.Verdict verdict, Level level) {
        Objects.requireNonNull(verdict, "verdict");
        Objects.requireNonNull(level, "level");
        double score = Math.max(0.0, Math.min(1.0, verdict.score()));
        double threshold = level.threshold();
        Action action;
        if (score >= threshold) {
            action = Action.BLOCK;
        } else if (score >= threshold - OBSERVATION_BAND) {
            action = Action.LOG;
        } else {
            action = Action.ALLOW;
        }
        return new Decision(level, score, action);
    }
}
