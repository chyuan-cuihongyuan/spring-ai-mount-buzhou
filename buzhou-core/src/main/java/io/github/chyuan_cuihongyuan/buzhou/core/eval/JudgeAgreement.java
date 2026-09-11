package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 双 judge 一致率（spec 541 / T833，scikit-learn cohen_kappa_score 思想
 * ——516 校准的双 judge 变体）：两个 judge 对同数据集的 verdict 一致性。
 * **Cohen's κ** 修正机遇一致（两 judge 都 95% 判绿时朴素一致率虚高——κ
 * 度量超出机遇的一致部分）。κ ≤ 0 无一致、0.6-0.8 实质一致、> 0.8 近乎
 * 完全一致（Landis-Koch 惯例）。
 *
 * <p>二值化：判红 = fail|error（516 同映射）；单侧项排除；纯函数。
 */
public final class JudgeAgreement {

    /** 一致报告（po=观测一致率、pe=机遇一致率、κ；分母 0 → null）。 */
    public record AgreementReport(int bothRed, int bothGreen, int aRedOnly, int bRedOnly,
            int singleSided, Double observedAgreement, Double chanceAgreement,
            Double kappa) {

        /** κ 惯例分级（Landis-Koch；null 透传）。 */
        public String strength() {
            if (kappa == null) {
                return "undefined";
            }
            if (kappa > 0.8) {
                return "almost-perfect";
            }
            if (kappa > 0.6) {
                return "substantial";
            }
            if (kappa > 0.4) {
                return "moderate";
            }
            if (kappa > 0.2) {
                return "fair";
            }
            return "slight-or-none";
        }
    }

    private JudgeAgreement() {
    }

    /** 一致性分析：judgeA/judgeB 同 itemId 对齐；单侧项排除。 */
    public static AgreementReport analyze(EvalRunResult judgeA, EvalRunResult judgeB) {
        if (judgeA == null || judgeB == null) {
            throw new IllegalArgumentException("两个 judge run 都必须非空");
        }
        Map<String, String> a = new LinkedHashMap<>();
        judgeA.items().forEach(i -> a.put(i.itemId(), i.status()));
        Map<String, String> b = new LinkedHashMap<>();
        judgeB.items().forEach(i -> b.put(i.itemId(), i.status()));

        int bothRed = 0;
        int bothGreen = 0;
        int aRedOnly = 0;
        int bRedOnly = 0;
        int singleSided = 0;
        for (String id : union(a.keySet(), b.keySet())) {
            String sa = a.get(id);
            String sb = b.get(id);
            if (sa == null || sb == null) {
                singleSided++;
                continue;
            }
            boolean redA = isRed(sa);
            boolean redB = isRed(sb);
            if (redA && redB) {
                bothRed++;
            } else if (!redA && !redB) {
                bothGreen++;
            } else if (redA) {
                aRedOnly++;
            } else {
                bRedOnly++;
            }
        }
        int n = bothRed + bothGreen + aRedOnly + bRedOnly;
        Double po = n == 0 ? null : (double) (bothRed + bothGreen) / n;
        // 机遇一致率 pe = P(A红)·P(B红) + P(A绿)·P(B绿)
        Double pe = null;
        if (n > 0) {
            double paRed = (double) (bothRed + aRedOnly) / n;
            double pbRed = (double) (bothRed + bRedOnly) / n;
            pe = paRed * pbRed + (1 - paRed) * (1 - pbRed);
        }
        Double kappa = (po == null || pe == null || pe >= 1.0) ? null
                : (po - pe) / (1 - pe);
        return new AgreementReport(bothRed, bothGreen, aRedOnly, bRedOnly, singleSided,
                po, pe, kappa);
    }

    private static boolean isRed(String status) {
        return EvalRunItemResult.STATUS_FAIL.equals(status)
                || EvalRunItemResult.STATUS_ERROR.equals(status);
    }

    private static java.util.Set<String> union(java.util.Set<String> a, java.util.Set<String> b) {
        java.util.Set<String> all = new java.util.LinkedHashSet<>(a);
        all.addAll(b);
        return all;
    }
}
