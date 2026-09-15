package io.github.chyuan_cuihongyuan.buzhou.tools;

import java.util.List;

/**
 * argv 预算门（spec 1845 / T2891 / impl 1446）——Linux execve ARG_MAX /
 * MAX_ARG_STRLEN 思想：命令行参数有两道系统级闸——**总量闸**（全部参数
 * 连 NUL 分隔的字节和，ARG_MAX 量级）与**单参闸**（单个参数不超
 * MAX_ARG_STRLEN 量级）——超限不是「能不能跑」的性能问题，是 E2BIG
 * 直接拒跑。工具侧先验后拼，把系统级拒绝提前到参数校验层。
 *
 * <p>纯函数零状态、只校验不执行（命令执行归宿主）。
 */
public final class ArgvBudgetGate {

    /** 总量预算（字节，ARG_MAX 量级默认）。 */
    public static final long DEFAULT_TOTAL_BUDGET_BYTES = 1_048_576L;

    /** 单参上限（字节，MAX_ARG_STRLEN 同量默认）。 */
    public static final int DEFAULT_SINGLE_ARG_CAP_BYTES = 131_072;

    private ArgvBudgetGate() {
    }

    /** 校验三态：FIT 放行 / OVER_TOTAL 超总量 / OVER_SINGLE_ARG 超单参。 */
    public enum Verdict {

        /** 两闸均过——可拼命令行。 */
        FIT,

        /** 总量超预算（E2BIG 总量型）。 */
        OVER_TOTAL,

        /** 单参超上限（E2BIG 单参型——先于总量判定）。 */
        OVER_SINGLE_ARG
    }

    /**
     * 字节账：每参计「字符数 + 1」（NUL 分隔），空表 0。
     */
    public static long totalBytes(List<String> args) {
        List<String> window = args == null ? List.of() : args;
        long total = 0;
        for (String arg : window) {
            total += arg.length() + 1;
        }
        return total;
    }

    /**
     * 双闸校验。契约：args 元素非 null（fail-fast）；单参闸**先于**总量闸
     *（单参超限的诊断价值高——直接指出哪类参数病）；边界含上（== 预算
     * /== 上限均 FIT）。
     */
    public static Verdict verify(List<String> args, long totalBudgetBytes,
                                 int singleArgCapBytes) {
        if (totalBudgetBytes < 0 || singleArgCapBytes < 1) {
            throw new IllegalArgumentException(String.format(
                    "非法闸参：total=%d, single=%d", totalBudgetBytes, singleArgCapBytes));
        }
        List<String> window = args == null ? List.of() : args;
        for (String arg : window) {
            if (arg == null) {
                throw new IllegalArgumentException("参数不能为 null");
            }
            if (arg.length() + 1 > singleArgCapBytes) {
                return Verdict.OVER_SINGLE_ARG;
            }
        }
        return totalBytes(window) > totalBudgetBytes
                ? Verdict.OVER_TOTAL
                : Verdict.FIT;
    }

    /** 默认闸参便捷入口。 */
    public static Verdict verify(List<String> args) {
        return verify(args, DEFAULT_TOTAL_BUDGET_BYTES, DEFAULT_SINGLE_ARG_CAP_BYTES);
    }
}
