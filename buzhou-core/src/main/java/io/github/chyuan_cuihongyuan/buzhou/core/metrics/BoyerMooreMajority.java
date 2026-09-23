package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.List;

/**
 * Boyer-Moore 多数表决（spec 4003 / T6007 / impl 2104）——O(1) 内存
 * 流式找多数元素思想（Boyer-Moore MJRTY 1991；rx-java/stream 底层
 * 同款）：配对抵消——候选与计数器两变量，同候选取证 +1、异候选取
 * 消 −1、归零换候选；**真多数（> n/2）必幸存**（每消一对至多废掉
 * 一张多数票），但幸存者未必多数——二次核验定夺。
 *
 * <p>「排序取中位数 O(n log n)／哈希计数 O(n) 内存」病的单遍常数
 * 内存解；投票语义（超半才算数——恰半是僵局不是多数）在配置
 * 版本灰度、评估标注一致性、错误签名主导性判定中天然合用。
 */
public final class BoyerMooreMajority {

    private BoyerMooreMajority() {
    }

    /**
     * 多数元素（出现次数 &gt; size/2）；无多数（含空表/恰半僵局）返回
     * null。配对抵消找幸存候选 + 二次核验定夺。
     */
    public static <T> T majorityOf(List<T> items) {
        if (items == null) {
            throw new IllegalArgumentException("items 非空");
        }
        T candidate = null;
        long counter = 0;
        for (T item : items) {
            if (counter == 0) {
                candidate = item;
                counter = 1;
            } else if (candidate.equals(item)) {
                counter++;
            } else {
                counter--;   // 配对抵消：候选一票 vs 当前一票同废
            }
        }
        if (candidate == null) {
            return null;   // 空表
        }
        long votes = 0;
        for (T item : items) {
            if (candidate.equals(item)) {
                votes++;
            }
        }
        return votes * 2 > items.size() ? candidate : null;
    }
}
