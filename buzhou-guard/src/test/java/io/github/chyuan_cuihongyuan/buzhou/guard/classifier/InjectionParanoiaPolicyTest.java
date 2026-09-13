package io.github.chyuan_cuihongyuan.buzhou.guard.classifier;

import io.github.chyuan_cuihongyuan.buzhou.guard.classifier.InjectionClassifier.Verdict;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 826 / T1154：paranoia 分级裁决回归——四档阈值表/BLOCK-LOG-ALLOW 三带/
 * 边界含等号/分数越界截断/fail-fast。
 */
class InjectionParanoiaPolicyTest {

    @Test
    void thresholdTableMatchesStandard() {
        assertThat(InjectionParanoiaPolicy.Level.L1.threshold()).isEqualTo(0.95);
        assertThat(InjectionParanoiaPolicy.Level.L2.threshold()).isEqualTo(0.85);
        assertThat(InjectionParanoiaPolicy.Level.L3.threshold()).isEqualTo(0.70);
        assertThat(InjectionParanoiaPolicy.Level.L4.threshold()).isEqualTo(0.50);
    }

    @Test
    void sameScoreDifferentLevelsDifferentVerdicts() {
        Verdict mid = new Verdict(true, 0.72, "suspicious-pattern");

        // 同一分数：L1/L2 放行、L3 拦截（0.72 ≥ 0.70）、L4 拦截
        assertThat(InjectionParanoiaPolicy.decide(mid, InjectionParanoiaPolicy.Level.L1).action())
                .isEqualTo(InjectionParanoiaPolicy.Action.ALLOW);
        assertThat(InjectionParanoiaPolicy.decide(mid, InjectionParanoiaPolicy.Level.L2).action())
                .isEqualTo(InjectionParanoiaPolicy.Action.ALLOW);
        assertThat(InjectionParanoiaPolicy.decide(mid, InjectionParanoiaPolicy.Level.L3).action())
                .isEqualTo(InjectionParanoiaPolicy.Action.BLOCK);
        assertThat(InjectionParanoiaPolicy.decide(mid, InjectionParanoiaPolicy.Level.L4).action())
                .isEqualTo(InjectionParanoiaPolicy.Action.BLOCK);
        // L3 观察带：0.65 ∈ [0.60, 0.70) → LOG
        assertThat(InjectionParanoiaPolicy.decide(new Verdict(true, 0.65, "s"),
                InjectionParanoiaPolicy.Level.L3).action())
                .isEqualTo(InjectionParanoiaPolicy.Action.LOG);
    }

    @Test
    void observationBandLogsBelowThreshold() {
        Verdict nearMiss = new Verdict(true, 0.88, "x");
        // L2 阈值 0.85：0.88 → BLOCK；0.80 → LOG（观察带 0.10）；0.70 → ALLOW
        assertThat(InjectionParanoiaPolicy.decide(nearMiss, InjectionParanoiaPolicy.Level.L2).action())
                .isEqualTo(InjectionParanoiaPolicy.Action.BLOCK);
        assertThat(InjectionParanoiaPolicy.decide(new Verdict(true, 0.80, "x"),
                InjectionParanoiaPolicy.Level.L2).action())
                .isEqualTo(InjectionParanoiaPolicy.Action.LOG);
        assertThat(InjectionParanoiaPolicy.decide(new Verdict(true, 0.70, "x"),
                InjectionParanoiaPolicy.Level.L2).action())
                .isEqualTo(InjectionParanoiaPolicy.Action.ALLOW);
    }

    @Test
    void boundaryEqualityBlocks() {
        Verdict exact = new Verdict(true, 0.85, "x");
        assertThat(InjectionParanoiaPolicy.decide(exact, InjectionParanoiaPolicy.Level.L2).action())
                .isEqualTo(InjectionParanoiaPolicy.Action.BLOCK); // >= 含等号
    }

    @Test
    void outOfRangeScoresClamped() {
        Verdict over = new Verdict(true, 1.7, "x");
        Verdict under = new Verdict(false, -0.5, "x");
        assertThat(InjectionParanoiaPolicy.decide(over, InjectionParanoiaPolicy.Level.L1).action())
                .isEqualTo(InjectionParanoiaPolicy.Action.BLOCK); // 1.7 截 1.0 ≥ 0.95
        assertThat(InjectionParanoiaPolicy.decide(under, InjectionParanoiaPolicy.Level.L4).action())
                .isEqualTo(InjectionParanoiaPolicy.Action.ALLOW); // -0.5 截 0 < 0.40
        // LOG 带下界（L4：0.40-0.50）
        assertThat(InjectionParanoiaPolicy.decide(new Verdict(false, 0.45, "x"),
                InjectionParanoiaPolicy.Level.L4).action())
                .isEqualTo(InjectionParanoiaPolicy.Action.LOG);
    }

    @Test
    void failFastOnNulls() {
        Verdict v = new Verdict(true, 0.9, "x");
        assertThatThrownBy(() -> InjectionParanoiaPolicy.decide(null, InjectionParanoiaPolicy.Level.L1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> InjectionParanoiaPolicy.decide(v, null))
                .isInstanceOf(NullPointerException.class);
    }
}
