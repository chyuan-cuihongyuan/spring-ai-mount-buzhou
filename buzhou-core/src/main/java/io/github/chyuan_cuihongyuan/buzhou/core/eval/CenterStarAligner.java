package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.ArrayList;
import java.util.List;

/**
 * 中心星法多序列比对（spec 10022 / X10045 / impl 2425）——Center-Star
 * 思想（「中心序列星形聚合」——ClustalW 渐进比对的前身形态）：**选
 * 与全体两两比对得分和最大的中心序列，其余各与中心做全局双序列
 * 比对后星形合并进 profile（中心插入空位列全列插空）**——免 n²
 * 维 profile DP 的多序列比对直接法。投影契约：每行去空位还原原序
 * 列；行等长契约；null/空集/空串 fail-fast；同输入同比对确定。
 */
public final class CenterStarAligner {

    /** 比对计分（匹配+1/错配−1/空位−2——经典简单核）。 */
    private static final int MATCH_SCORE = 1;
    private static final int MISMATCH_SCORE = -1;
    private static final int GAP_SCORE = -2;

    private CenterStarAligner() {
    }

    /**
     * 多序列比对（返回行等长字符串表，行序与输入一致）。
     *
     * @throws IllegalArgumentException null/空集/空串元素
     */
    public static List<String> align(List<String> sequences) {
        if (sequences == null || sequences.isEmpty()) {
            throw new IllegalArgumentException("序列集非空非 null");
        }
        for (String s : sequences) {
            if (s == null || s.isEmpty()) {
                throw new IllegalArgumentException("序列元素非空串非 null");
            }
        }
        if (sequences.size() == 1) {
            return new ArrayList<>(sequences);
        }
        int center = pickCenter(sequences);
        String centerSeq = sequences.get(center);
        List<Integer> order = new ArrayList<>();
        order.add(center);
        for (int i = 0; i < sequences.size(); i++) {
            if (i != center) {
                order.add(i);
            }
        }
        List<StringBuilder> profile = new ArrayList<>();
        profile.add(new StringBuilder(centerSeq));
        for (int idx = 1; idx < order.size(); idx++) {
            String other = sequences.get(order.get(idx));
            String[] pair = globalAlign(centerSeq, other);
            profile = merge(profile, pair[0], pair[1]);
        }
        String[] byInput = new String[sequences.size()];
        for (int idx = 0; idx < order.size(); idx++) {
            byInput[order.get(idx)] = profile.get(idx).toString();
        }
        List<String> result = new ArrayList<>();
        for (String row : byInput) {
            result.add(row);
        }
        return result;
    }

    /** 中心选择（两两 NW 得分和最大——平局取首序）。 */
    private static int pickCenter(List<String> sequences) {
        int best = 0;
        long bestScore = Long.MIN_VALUE;
        for (int i = 0; i < sequences.size(); i++) {
            long total = 0;
            for (int j = 0; j < sequences.size(); j++) {
                if (i != j) {
                    total += alignScore(sequences.get(i), sequences.get(j));
                }
            }
            if (total > bestScore) {
                bestScore = total;
                best = i;
            }
        }
        return best;
    }

    /** Needleman–Wunsch 全局比对得分（评分面）。 */
    private static int alignScore(String first, String second) {
        int[][] dp = scoreMatrix(first, second);
        return dp[first.length()][second.length()];
    }

    private static int[][] scoreMatrix(String first, String second) {
        int[][] dp = new int[first.length() + 1][second.length() + 1];
        for (int i = 1; i <= first.length(); i++) {
            dp[i][0] = dp[i - 1][0] + GAP_SCORE;
        }
        for (int j = 1; j <= second.length(); j++) {
            dp[0][j] = dp[0][j - 1] + GAP_SCORE;
        }
        for (int i = 1; i <= first.length(); i++) {
            for (int j = 1; j <= second.length(); j++) {
                int diag = dp[i - 1][j - 1]
                        + (first.charAt(i - 1) == second.charAt(j - 1) ? MATCH_SCORE : MISMATCH_SCORE);
                int up = dp[i - 1][j] + GAP_SCORE;
                int left = dp[i][j - 1] + GAP_SCORE;
                dp[i][j] = Math.max(diag, Math.max(up, left));
            }
        }
        return dp;
    }

    /** 全局比对回溯（对角→上→左确定平局序），返回 {比对后中心, 比对后他序}。 */
    private static String[] globalAlign(String first, String second) {
        int[][] dp = scoreMatrix(first, second);
        StringBuilder a = new StringBuilder();
        StringBuilder b = new StringBuilder();
        int i = first.length();
        int j = second.length();
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1]
                    + (first.charAt(i - 1) == second.charAt(j - 1) ? MATCH_SCORE : MISMATCH_SCORE)) {
                a.append(first.charAt(i - 1));
                b.append(second.charAt(j - 1));
                i--;
                j--;
            } else if (i > 0 && dp[i][j] == dp[i - 1][j] + GAP_SCORE) {
                a.append(first.charAt(i - 1));
                b.append('-');
                i--;
            } else {
                a.append('-');
                b.append(second.charAt(j - 1));
                j--;
            }
        }
        return new String[]{a.reverse().toString(), b.reverse().toString()};
    }

    /** 星形合并（中心空位列全列插空——他序空位仅自带）。 */
    private static List<StringBuilder> merge(List<StringBuilder> profile,
                                             String alignedCenter, String alignedOther) {
        String currentCenter = profile.get(0).toString();
        List<StringBuilder> merged = new ArrayList<>();
        for (int r = 0; r <= profile.size(); r++) {
            merged.add(new StringBuilder());
        }
        int p = 0;
        int q = 0;
        while (p < currentCenter.length() || q < alignedCenter.length()) {
            char profileChar = p < currentCenter.length() ? currentCenter.charAt(p) : '-';
            char pairChar = q < alignedCenter.length() ? alignedCenter.charAt(q) : '-';
            if (profileChar != '-' && pairChar != '-') {
                // 同一中心碱基（双方非空位必同字）
                merged.get(0).append(profileChar);
                for (int r = 1; r < profile.size(); r++) {
                    merged.get(r).append(profile.get(r).charAt(p));
                }
                merged.get(profile.size()).append(alignedOther.charAt(q));
                p++;
                q++;
            } else if (profileChar == '-' && pairChar != '-') {
                // profile 旧空位列——新序列此处无碱基
                merged.get(0).append('-');
                for (int r = 1; r < profile.size(); r++) {
                    merged.get(r).append(profile.get(r).charAt(p));
                }
                merged.get(profile.size()).append('-');
                p++;
            } else if (profileChar != '-') {
                // 比对新插中心空位列——全列插空
                merged.get(0).append('-');
                for (int r = 1; r < profile.size(); r++) {
                    merged.get(r).append('-');
                }
                merged.get(profile.size()).append(alignedOther.charAt(q));
                q++;
            } else {
                // 双空位（对齐尾垫平）——收缩单列
                merged.get(0).append('-');
                for (int r = 1; r < profile.size(); r++) {
                    merged.get(r).append(profile.get(r).charAt(p));
                }
                merged.get(profile.size()).append(alignedOther.charAt(q));
                p++;
                q++;
            }
        }
        return merged;
    }
}
