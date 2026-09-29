package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bitap 位并行搜索（spec 8002 / V8005 / impl 2304）——
 * Baeza-Yates & Gonnet 1992 思想（agrep/ripgrep 同源）：
 * **模式匹配状态压进一个机器字按位并行推进**——状态
 * {@code state=(state<<1|1)&mask[c]} 单字转移，命中=状态
 * 高位 1——短模式下逐字符状态机分支密集（分支预测失败
 * 常数放大）的病解。模式长 ≤63（long 位宽上限——越界诚实
 * 拒绝而非静默错配）；findAll 全部（含重叠）命中；确定性
 * 纯函数；null/空模式 fail-fast。
 *
 * <p>与 KmpSearch（spec 3014）、BoyerMooreSearch（spec 8001）
 * 同族不同面：失配函数 vs 双启发滑动 vs 位并行单字推进。
 */
public final class BitapSearch {

    /** long 位宽上限：模式超过 63 字符无法单字并行（诚实拒绝）。 */
    private static final int MAX_PATTERN_LENGTH = 63;

    private BitapSearch() {
    }

    /** 全部（含重叠）命中起始位升序（null/空模式或超位宽 fail-fast）。 */
    public static List<Integer> findAll(String text, String pattern) {
        if (text == null || pattern == null) {
            throw new IllegalArgumentException("文本与模式均非空引用");
        }
        if (pattern.isEmpty()) {
            throw new IllegalArgumentException("模式非空串（空模式命中语义无定义）");
        }
        int m = pattern.length();
        if (m > MAX_PATTERN_LENGTH) {
            throw new IllegalArgumentException("模式长 " + m + " 超 long 位宽上限 " + MAX_PATTERN_LENGTH);
        }
        Map<Character, Long> masks = new HashMap<>();
        for (int i = 0; i < m; i++) {
            masks.merge(pattern.charAt(i), 1L << i, (a, b) -> a | b);
        }
        long acceptBit = 1L << (m - 1);
        List<Integer> hits = new ArrayList<>();
        long state = 0;
        for (int i = 0; i < text.length(); i++) {
            state = (state << 1 | 1L) & masks.getOrDefault(text.charAt(i), 0L);
            if ((state & acceptBit) != 0) {
                hits.add(i - m + 1);
            }
        }
        return List.copyOf(hits);
    }

    /** 首个命中位（无命中 −1 诚实缺省）。 */
    public static int first(String text, String pattern) {
        List<Integer> hits = findAll(text, pattern);
        return hits.isEmpty() ? -1 : hits.get(0);
    }
}
