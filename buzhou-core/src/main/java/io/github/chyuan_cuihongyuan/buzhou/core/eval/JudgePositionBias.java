package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.List;

/**
 * 裁判位置偏差读面（L 会话 1700 系 R4 = effort #1703 / spec 1703 /
 * 票 T2607 + T2608 / impl 1303）——MT-Bench / FastChat 的裁判位置偏差
 * 检验思想：成对裁判对 (A,B) 与换位 (B,A) 的裁决应当镜像一致；
 * 「谁站第一位谁赢」的换位不服即位置偏差。
 *
 * <p>纯函数零状态：吃成对裁决记录（每例含 id 与正/换位两次裁决，裁决域
 * A/B/TIE），吐一致/首位双赢/次位双赢/混合平四桶与偏差比（−1 哨兵）。
 *
 * @since 1.0.0
 */
public final class JudgePositionBias {

    private JudgePositionBias() {
    }

    /** 成对裁决闭集：A 胜 / B 胜 / 平。 */
    public enum Verdict { A, B, TIE }

    /**
     * @param id          用例标识
     * @param verdictAB   正序 (A,B) 裁决
     * @param verdictBA   换位 (B,A) 裁决
     */
    public record PairJudgement(String id, Verdict verdictAB, Verdict verdictBA) {
    }

    /**
     * @param pairs          例数
     * @param consistent     镜像一致例数（AB=A&BA=B / AB=B&BA=A / 双 TIE）
     * @param firstWinsBoth  首位双赢例数（AB=A&BA=A——位置偏差信号）
     * @param secondWinsBoth 次位双赢例数（AB=B&BA=B——位置偏差信号）
     * @param mixedTie       平局不一致例数（恰一次 TIE）
     * @param biasRatio      位置偏差比（first+second 双赢)/pairs；pairs=0 哨兵 −1
     */
    public record BiasReport(int pairs, int consistent, int firstWinsBoth,
                             int secondWinsBoth, int mixedTie, double biasRatio) {
    }

    /** 审计入口：成对裁决记录列表。 */
    public static BiasReport analyze(List<PairJudgement> judgements) {
        int consistent = 0;
        int firstWins = 0;
        int secondWins = 0;
        int mixedTie = 0;
        List<PairJudgement> data = judgements == null ? List.of() : judgements;
        for (PairJudgement j : data) {
            Verdict ab = j.verdictAB();
            Verdict ba = j.verdictBA();
            if (ab == Verdict.TIE && ba == Verdict.TIE) {
                consistent++;
            } else if (ab == Verdict.A && ba == Verdict.B) {
                consistent++;
            } else if (ab == Verdict.B && ba == Verdict.A) {
                consistent++;
            } else if (ab == Verdict.A && ba == Verdict.A) {
                firstWins++;
            } else if (ab == Verdict.B && ba == Verdict.B) {
                secondWins++;
            } else {
                mixedTie++;
            }
        }
        int pairs = data.size();
        double biasRatio = pairs == 0 ? -1d
                : (double) (firstWins + secondWins) / pairs;
        return new BiasReport(pairs, consistent, firstWins, secondWins, mixedTie, biasRatio);
    }
}
