package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.HashMap;
import java.util.Map;

/**
 * 最小覆盖子串（spec 8014 / V8029 / impl 2316）——
 * 滑动窗口经典思想（CLRS/竞赛最覆盖面同源）：**need 计数表
 * + window 计数表 + satisfied 双指针伸缩**——右界扩张补满足、
 * 左界收缩去冗余，右界推进均摊 O(n)、窗口合法性 O(1) 维护
 * ——全起点全终点枚举 O(n²m)（文本放大）的病解。并列取最左
 * （canonical——同输入同结果）；无覆盖返回空串（诚实缺省）；
 * null/空模式 fail-fast；确定性纯函数。
 *
 * <p>与 BoyerMooreSearch（spec 8001）同族不同面：精确子串
 * 定位 vs 多重需求覆盖窗口。
 */
public final class MinWindowSubstring {

    private MinWindowSubstring() {
    }

    /** 最小覆盖窗口（无覆盖空串诚实缺省；null/空模式 fail-fast）。 */
    public static String minWindow(String text, String pattern) {
        if (text == null || pattern == null) {
            throw new IllegalArgumentException("文本与模式均非空引用");
        }
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("模式非空串（覆盖语义无定义）");
        }
        Map<Character, Integer> need = new HashMap<>();
        for (int i = 0; i < pattern.length(); i++) {
            need.merge(pattern.charAt(i), 1, Integer::sum);
        }
        int missing = pattern.length();
        int bestLeft = -1;
        int bestLength = Integer.MAX_VALUE;
        int left = 0;
        Map<Character, Integer> window = new HashMap<>();
        for (int right = 0; right < text.length(); right++) {
            char incoming = text.charAt(right);
            Integer needed = need.get(incoming);
            if (needed == null) {
                continue;
            }
            int count = window.merge(incoming, 1, Integer::sum);
            if (count <= needed) {
                missing--;
            }
            while (missing == 0) {
                int length = right - left + 1;
                if (length < bestLength) {
                    bestLength = length;
                    bestLeft = left;
                }
                char outgoing = text.charAt(left);
                Integer leftNeeded = need.get(outgoing);
                if (leftNeeded != null) {
                    int leftCount = window.merge(outgoing, -1, Integer::sum);
                    if (leftCount < leftNeeded) {
                        missing++;
                    }
                }
                left++;
            }
        }
        return bestLeft < 0 ? "" : text.substring(bestLeft, bestLeft + bestLength);
    }
}
