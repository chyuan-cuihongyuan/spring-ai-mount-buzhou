package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 技能排序一致性读面（L 会话 1700 系 R36 = effort #1735 / spec 1735 /
 * 票 T2671 + T2672 / impl 1335）——scikit-learn 的 Kendall τ / 排序一致性
 * 评估思想：词法（{@link LexicalSkillRanker}）与语义
 * （{@link SemanticSkillRanker}）两排序器对同一查询的排序若高度一致，
 * 双路归一可简化；分歧大则语义路在贡献真实信号——τ 一数定分歧。
 *
 * <p>纯函数零状态：`tau(rankA, rankB)` 在两排序的**公共项**上算 Kendall τ
 * = (C−D)/(C+D)（C 一致对/D 相反对）；公共项 &lt;2 哨兵 −1。值域 [−1,1]。
 *
 * @since 1.0.0
 */
public final class SkillRankAgreement {

    private SkillRankAgreement() {
    }

    /**
     * @param commonItems 公共项数
     * @param concordant  一致对数
     * @param discordant  相反对数
     * @param tau         Kendall τ（公共项 &lt;2 哨兵 −1）
     */
    public record Agreement(int commonItems, int concordant, int discordant, double tau) {
    }

    /** 一致性入口：两个有序列表（前者 A 排序，后者 B 排序）。 */
    public static Agreement tau(List<String> rankA, List<String> rankB) {
        List<String> a = rankA == null ? List.of() : rankA;
        List<String> b = rankB == null ? List.of() : rankB;
        Set<String> common = new HashSet<>(a);
        common.retainAll(new HashSet<>(b));
        int n = common.size();
        if (n < 2) {
            return new Agreement(n, 0, 0, -1d);
        }
        List<String> ordered = new ArrayList<>(common);
        ordered.sort(String::compareTo);
        List<Integer> positions = new ArrayList<>(n);
        for (String item : ordered) {
            positions.add(b.indexOf(item));
        }
        int concordant = 0;
        int discordant = 0;
        for (int i = 0; i < positions.size(); i++) {
            for (int j = i + 1; j < positions.size(); j++) {
                if (positions.get(i) < positions.get(j)) {
                    concordant++;
                } else {
                    discordant++;
                }
            }
        }
        double tau = (double) (concordant - discordant) / (concordant + discordant);
        return new Agreement(n, concordant, discordant, tau);
    }
}
