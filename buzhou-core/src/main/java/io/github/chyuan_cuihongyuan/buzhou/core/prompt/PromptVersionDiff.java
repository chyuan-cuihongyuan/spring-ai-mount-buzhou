package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * 提示词版本行级 diff（spec 534 / T821，Git diff 思想——401 注册表扩散）：
 * 两个版本正文按行对齐的最小变更集（LCS）。晋级/回滚评审「到底改了什么」
 * 的读数面。纯函数。
 *
 * <p>诚实边界：行级 LCS（不做词内 diff/语义 diff）；输出面向人审。
 */
public final class PromptVersionDiff {

    /** 单行变更（context=equal|insert|delete）。 */
    public record DiffLine(String context, String line) {
    }

    /** diff 报告（两版本号 + 变更行全集；equal 行保序保留——评审可读）。 */
    public record VersionDiff(int fromVersion, int toVersion,
            List<DiffLine> lines, int added, int removed) {

        /** 净变化（正=增行多）。 */
        public int net() {
            return added - removed;
        }
    }

    private PromptVersionDiff() {
    }

    /** 版本间行级 LCS diff（两版本须同名——异名 IllegalArgumentException）。 */
    public static VersionDiff diff(PromptVersion from, PromptVersion to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("两个版本都必须非空");
        }
        if (!from.name().equals(to.name())) {
            throw new IllegalArgumentException("版本名不一致：" + from.name() + " vs " + to.name());
        }
        String[] a = from.body().split("\n", -1);
        String[] b = to.body().split("\n", -1);
        int[][] lcs = lcsTable(a, b);
        List<DiffLine> lines = new ArrayList<>();
        int added = 0;
        int removed = 0;
        int i = 0;
        int j = 0;
        while (i < a.length && j < b.length) {
            if (a[i].equals(b[j])) {
                lines.add(new DiffLine("equal", a[i]));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                lines.add(new DiffLine("delete", a[i]));
                removed++;
                i++;
            } else {
                lines.add(new DiffLine("insert", b[j]));
                added++;
                j++;
            }
        }
        while (i < a.length) {
            lines.add(new DiffLine("delete", a[i]));
            removed++;
            i++;
        }
        while (j < b.length) {
            lines.add(new DiffLine("insert", b[j]));
            added++;
            j++;
        }
        return new VersionDiff(from.version(), to.version(), List.copyOf(lines), added, removed);
    }

    /** 经典 LCS 长度表（(n+1)×(m+1)，dp[i][j] = a[i..] 与 b[j..] 的 LCS 长度）。 */
    private static int[][] lcsTable(String[] a, String[] b) {
        int[][] dp = new int[a.length + 1][b.length + 1];
        for (int i = a.length - 1; i >= 0; i--) {
            for (int j = b.length - 1; j >= 0; j--) {
                dp[i][j] = a[i].equals(b[j]) ? dp[i + 1][j + 1] + 1
                        : Math.max(dp[i + 1][j], dp[i][j + 1]);
            }
        }
        return dp;
    }
}
