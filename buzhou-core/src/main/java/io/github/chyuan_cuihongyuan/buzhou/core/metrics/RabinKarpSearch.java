package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Rabin-Karp 滚动哈希搜索（spec 3025 / T5051 / impl 2026）——
 * Karp-Rabin 1987 思想：窗口多项式哈希**滚动递推**——
 * h' = (h − 左出字符·B^(m−1))·B + 右进字符，O(1) 更新免整窗
 * 重哈希（滑窗全窗重哈希 O(n·m) 病的根治）；哈希命中后**逐字
 * 复核**（Las Vegas——假阳命中零误报）。滚动哈希口径可复用到
 * 滑窗指纹/去重（复用侧价值）；与 KmpSearch 互补：KMP 单模式
 * 确定失配跳跃，本件贡献可复用滚动指纹。
 *
 * <p>long 溢出环绕即隐式取模（免显式 mod 与负数陷阱——Java
 * 环绕语义确定性）；空模式双约定与 KmpSearch 对齐。
 */
public final class RabinKarpSearch {

    /** 多项式哈希基（char 全域 256 覆盖）。 */
    private static final long HASH_BASE = 256;

    private RabinKarpSearch() {
    }

    /** 首次匹配下标（无匹配 −1；空模式 0——JDK/KmpSearch 同约定）。 */
    public static int indexOf(String text, String pattern) {
        requireNonNull(text, pattern);
        if (pattern.isEmpty()) {
            return 0;
        }
        long patternHash = hashOf(pattern);
        long highOrder = highestOrderFactor(pattern.length());
        long windowHash = 0;
        int n = text.length();
        int m = pattern.length();
        for (int i = 0; i < n; i++) {
            if (i >= m) {
                windowHash -= text.charAt(i - m) * highOrder;
            }
            windowHash = windowHash * HASH_BASE + text.charAt(i);
            if (i >= m - 1 && windowHash == patternHash
                    && text.regionMatches(i - m + 1, pattern, 0, m)) {
                return i - m + 1;
            }
        }
        return -1;
    }

    /** 全部匹配起点（可重叠；哈希命中逐字复核——零误报）。 */
    public static List<Integer> findAll(String text, String pattern) {
        requireNonNull(text, pattern);
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("findAll 空模式无穷匹配——拒绝");
        }
        long patternHash = hashOf(pattern);
        long highOrder = highestOrderFactor(pattern.length());
        List<Integer> hits = new ArrayList<>();
        long windowHash = 0;
        int n = text.length();
        int m = pattern.length();
        for (int i = 0; i < n; i++) {
            if (i >= m) {
                windowHash -= text.charAt(i - m) * highOrder;
            }
            windowHash = windowHash * HASH_BASE + text.charAt(i);
            if (i >= m - 1 && windowHash == patternHash
                    && text.regionMatches(i - m + 1, pattern, 0, m)) {
                hits.add(i - m + 1);
            }
        }
        return List.copyOf(hits);
    }

    /** 内容哈希（同内容同哈希——滚动口径的锚点/对账读数面）。 */
    public static long hashOf(String s) {
        long hash = 0;
        for (int i = 0; i < s.length(); i++) {
            hash = hash * HASH_BASE + s.charAt(i);
        }
        return hash;
    }

    /** BASE^(length−1)（最高位因子——左出字符的量级）。 */
    private static long highestOrderFactor(int length) {
        long factor = 1;
        for (int i = 1; i < length; i++) {
            factor *= HASH_BASE;
        }
        return factor;
    }

    private static void requireNonNull(String text, String pattern) {
        if (text == null || pattern == null) {
            throw new IllegalArgumentException("text/pattern 非空");
        }
    }
}
