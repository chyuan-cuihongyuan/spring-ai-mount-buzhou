package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 三方合并（spec 4042 / T6085 / impl 2143）——git merge-file /
 * diff3 思想：以基准（base）为锚，对「我方（ours）」与「他方
 * （theirs）」各做 LCS 行级 diff，改动按 base 区间对账——
 * <ul>
 *   <li>单侧改动 → 自动取该侧；</li>
 *   <li>双侧改动**相同内容** → 自动归一（git 同款：同改不算冲突）；</li>
 *   <li>双侧改动**相接或重叠且不同** → 冲突（保守口径：相邻也
 *    算——先后次序不可辨）。</li>
 * </ul>
 * 冲突块以 git 风格标记物化进结果行，同时以结构化
 * {@link Conflict} 暴露两侧原文——并发编辑后写全量覆盖（先写
 * 者编辑丢失）的病解。确定性：同输入同输出。
 *
 * <p>与 JSON Patch（RFC 6902）同族不同面：操作重放 vs 状态
 * 对账合并。
 */
public final class ThreeWayMerge {

    private static final String OURS_MARKER = "<<<<<<< ours";
    private static final String SEPARATOR_MARKER = "=======";
    private static final String THEIRS_MARKER = ">>>>>>> theirs";

    private ThreeWayMerge() {
    }

    /**
     * 三方合并（入参非 null；行序列可为空）。
     *
     * @param base 基准行
     * @param ours 我方行
     * @param theirs 他方行
     * @return 合并结果（结果行 + 冲突清单）
     */
    public static Outcome merge(List<String> base, List<String> ours, List<String> theirs) {
        if (base == null || ours == null || theirs == null) {
            throw new IllegalArgumentException("base/ours/theirs 非 null");
        }
        List<Hunk> oursHunks = diff(base, ours);
        List<Hunk> theirsHunks = diff(base, theirs);
        List<String> lines = new ArrayList<>();
        List<Conflict> conflicts = new ArrayList<>();
        int position = 0;
        int oursIndex = 0;
        int theirsIndex = 0;
        while (oursIndex < oursHunks.size() || theirsIndex < theirsHunks.size()) {
            boolean takeOurs = theirsIndex >= theirsHunks.size()
                    || (oursIndex < oursHunks.size()
                        && oursHunks.get(oursIndex).baseStart() <= theirsHunks.get(theirsIndex).baseStart());
            Hunk primary = takeOurs ? oursHunks.get(oursIndex) : theirsHunks.get(theirsIndex);
            Hunk secondary = takeOurs && theirsIndex < theirsHunks.size()
                    ? theirsHunks.get(theirsIndex)
                    : (!takeOurs && oursIndex < oursHunks.size() ? oursHunks.get(oursIndex) : null);
            if (secondary == null || !overlaps(primary, secondary)) {
                lines.addAll(unchanged(base, position, primary.baseStart()));
                lines.addAll(primary.replacement());
                position = primary.baseEnd();
                if (takeOurs) {
                    oursIndex++;
                } else {
                    theirsIndex++;
                }
                continue;
            }
            // 相接/重叠——传递聚合成冲突区（相邻同侧 hunk 一并吸入）
            int unionStart = Math.min(primary.baseStart(), secondary.baseStart());
            int unionEnd = Math.max(primary.baseEnd(), secondary.baseEnd());
            if (takeOurs) {
                oursIndex++;
            } else {
                theirsIndex++;
            }
            boolean extended = true;
            while (extended) {
                extended = false;
                while (oursIndex < oursHunks.size() && oursHunks.get(oursIndex).baseStart() <= unionEnd) {
                    Hunk absorbed = oursHunks.get(oursIndex++);
                    unionStart = Math.min(unionStart, absorbed.baseStart());
                    unionEnd = Math.max(unionEnd, absorbed.baseEnd());
                    extended = true;
                }
                while (theirsIndex < theirsHunks.size()
                        && theirsHunks.get(theirsIndex).baseStart() <= unionEnd) {
                    Hunk absorbed = theirsHunks.get(theirsIndex++);
                    unionStart = Math.min(unionStart, absorbed.baseStart());
                    unionEnd = Math.max(unionEnd, absorbed.baseEnd());
                    extended = true;
                }
            }
            lines.addAll(unchanged(base, position, unionStart));
            List<String> oursLines = sideLines(base, oursHunks, unionStart, unionEnd);
            List<String> theirsLines = sideLines(base, theirsHunks, unionStart, unionEnd);
            if (oursLines.equals(theirsLines)) {
                lines.addAll(oursLines);   // 同改归一——不算冲突（git 同款）
            } else {
                lines.add(OURS_MARKER);
                lines.addAll(oursLines);
                lines.add(SEPARATOR_MARKER);
                lines.addAll(theirsLines);
                lines.add(THEIRS_MARKER);
                conflicts.add(new Conflict(List.copyOf(oursLines), List.copyOf(theirsLines)));
            }
            position = unionEnd;
        }
        lines.addAll(unchanged(base, position, base.size()));
        return new Outcome(List.copyOf(lines), List.copyOf(conflicts));
    }

    /** 两侧是否相接或重叠（闭闭区间相碰即真——保守口径）。 */
    private static boolean overlaps(Hunk first, Hunk second) {
        return first.baseStart() <= second.baseEnd() && second.baseStart() <= first.baseEnd();
    }

    /** base[from, to) 原样片段（未改动区）。 */
    private static List<String> unchanged(List<String> base, int from, int to) {
        return base.subList(Math.min(from, base.size()), Math.min(to, base.size()));
    }

    /** 侧在 base [from,to) 区间的投影（未改动走 base、改动走替换行；零宽插入 hunk 特判）。 */
    private static List<String> sideLines(List<String> base, List<Hunk> hunks, int from, int to) {
        List<String> lines = new ArrayList<>();
        int position = from;
        int index = 0;
        while (position < to) {
            Hunk current = index < hunks.size() ? hunks.get(index) : null;
            if (current != null && current.baseStart() == current.baseEnd()
                    && current.baseStart() == position) {
                lines.addAll(current.replacement());   // 零宽插入——emit 且不推进 base 位
                index++;
                continue;
            }
            if (current != null && current.baseStart() <= position && position < current.baseEnd()) {
                lines.addAll(current.replacement());
                position = current.baseEnd();
                index++;
                continue;
            }
            if (current != null && current.baseStart() <= position) {
                index++;
                continue;
            }
            lines.add(base.get(position));
            position++;
        }
        // 右端零宽插入（from==to 的空区间并入此口）——插入内容在边界上仍需参与投影
        for (int k = index; k < hunks.size(); k++) {
            Hunk edge = hunks.get(k);
            if (edge.baseStart() == to && edge.baseStart() == edge.baseEnd()) {
                lines.addAll(edge.replacement());
            }
        }
        return lines;
    }

    /** LCS 行级 diff（确定性走格：同长优先删）产出改动 hunk。 */
    private static List<Hunk> diff(List<String> base, List<String> target) {
        int[][] lcs = new int[base.size() + 1][target.size() + 1];
        for (int i = base.size() - 1; i >= 0; i--) {
            for (int j = target.size() - 1; j >= 0; j--) {
                lcs[i][j] = Objects.equals(base.get(i), target.get(j))
                        ? lcs[i + 1][j + 1] + 1
                        : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        List<Hunk> hunks = new ArrayList<>();
        int i = 0;
        int j = 0;
        int runStart = -1;
        List<String> runInserts = new ArrayList<>();
        while (i < base.size() || j < target.size()) {
            if (i < base.size() && j < target.size()
                    && Objects.equals(base.get(i), target.get(j))) {
                if (runStart >= 0) {
                    hunks.add(new Hunk(runStart, i, List.copyOf(runInserts)));
                    runStart = -1;
                    runInserts = new ArrayList<>();
                }
                i++;
                j++;
                continue;
            }
            if (runStart < 0) {
                runStart = i;
            }
            if (j >= target.size() || (i < base.size() && lcs[i + 1][j] >= lcs[i][j + 1])) {
                i++;   // 删除 base[i]
            } else {
                runInserts.add(target.get(j));
                j++;   // 插入 target[j]
            }
        }
        if (runStart >= 0) {
            hunks.add(new Hunk(runStart, i, List.copyOf(runInserts)));
        }
        return hunks;
    }

    /**
     * 合并结果。
     *
     * @param lines 合并后的行（冲突块已物化为标记行）
     * @param conflicts 冲突清单（空 = 干净合并）
     */
    public record Outcome(List<String> lines, List<Conflict> conflicts) {
    }

    /**
     * 冲突块。
     *
     * @param oursLines 我方原文
     * @param theirsLines 他方原文
     */
    public record Conflict(List<String> oursLines, List<String> theirsLines) {
    }

    /** base 区间改动（[baseStart, baseEnd) → replacement）。 */
    private record Hunk(int baseStart, int baseEnd, List<String> replacement) {
    }
}
