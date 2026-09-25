package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Myers O(ND) Diff（spec 6007 / T6215 / impl 2208）——
 * Myers 差分论文思想（git diff 同源）：**贪心 O(ND)** 最短
 * 编辑脚本——外层步数 d、对角线 k 的 V 数组推进，每步快照
 * 轨迹供回溯产出三态脚本（EQUAL/INSERT/DELETE），脚本长度
 * = N+M−2·LCS（最优）——全量替换（差异无结构不可审）与
 * 朴素全量 DP O(N·M) 时空（大序列不可行）的病解。tie-break
 * 定构（k=−d 先降后删——同输入同脚本可回放）。
 *
 * <p>与 ThreeWayMerge（spec 4042）同族不同面：双侧 diff
 * 底座 vs 三方归一冲突显形。静态工具面（无状态）。
 */
public final class MyersDiff {

    /** 脚本操作。 */
    public enum Op {
        EQUAL, INSERT, DELETE
    }

    /** 单条编辑（text 为该操作涉及的行文本）。 */
    public record Edit(Op op, String text) {
    }

    private MyersDiff() {
    }

    /** 最短编辑脚本（null 序列 fail-fast；脚本应用于 a 恒得 b）。 */
    public static List<Edit> diff(List<String> a, List<String> b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("序列非空");
        }
        int n = a.size();
        int m = b.size();
        if (n == 0 && m == 0) {
            return new ArrayList<>();
        }
        int max = n + m;
        int offset = max;
        int[] v = new int[2 * max + 1];
        List<int[]> trace = new ArrayList<>();
        int foundD = -1;
        outer:
        for (int d = 0; d <= max; d++) {
            trace.add(v.clone());
            for (int k = -d; k <= d; k += 2) {
                int x;
                if (k == -d || (k != d && v[k - 1 + offset] < v[k + 1 + offset])) {
                    x = v[k + 1 + offset];
                } else {
                    x = v[k - 1 + offset] + 1;
                }
                int y = x - k;
                while (x < n && y < m && a.get(x).equals(b.get(y))) {
                    x++;
                    y++;
                }
                v[k + offset] = x;
                if (x >= n && y >= m) {
                    foundD = d;
                    break outer;
                }
            }
        }
        List<Edit> script = new ArrayList<>();
        int x = n;
        int y = m;
        for (int d = foundD; d >= 0; d--) {
            int[] snapshot = trace.get(d);
            int k = x - y;
            int prevK;
            if (k == -d || (k != d && snapshot[k - 1 + offset] < snapshot[k + 1 + offset])) {
                prevK = k + 1;
            } else {
                prevK = k - 1;
            }
            int prevX = snapshot[prevK + offset];
            int prevY = prevX - prevK;
            while (x > prevX && y > prevY) {
                script.add(new Edit(Op.EQUAL, a.get(x - 1)));
                x--;
                y--;
            }
            if (d > 0) {
                if (x == prevX) {
                    script.add(new Edit(Op.INSERT, b.get(y - 1)));
                    y--;
                } else {
                    script.add(new Edit(Op.DELETE, a.get(x - 1)));
                    x--;
                }
            }
        }
        java.util.Collections.reverse(script);
        return script;
    }
}
