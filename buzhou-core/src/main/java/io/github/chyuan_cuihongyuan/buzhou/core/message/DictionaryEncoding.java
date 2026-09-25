package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Dictionary Encoding 字典编码（spec 6017 / T6233 / impl 2217）——
 * Parquet/ORC 列存字典编码思想：**低基数列拆「字典表+变窄
 * 下标列」**——首次出现序登记字典（值→下标），下标列以
 * ⌈log₂(去重数)⌉ 位定宽打包（组合 {@link BitPacking}）——
 * 重复率高的列（枚举/状态/标签）逐值全宽直存（存储放大）
 * 的病解。层间解耦（字典面 vs 下标面各自可审计）。
 *
 * <p>与 BitPacking（T16）组合不同层：下标列的底座编码；
 * 与 Simple8b（T15）不同面：基数收缩换窄域 vs 同域变长
 * 混合打包。静态定构（首次出现序——同列同字典）。
 */
public final class DictionaryEncoding {

    private final long[] dictionary;
    private final BitPacking indices;

    private DictionaryEncoding(long[] dictionary, BitPacking indices) {
        this.dictionary = dictionary;
        this.indices = indices;
    }

    /** 编码列（null/空 fail-fast）。 */
    public static DictionaryEncoding encode(long[] column) {
        if (column == null || column.length == 0) {
            throw new IllegalArgumentException("列非空");
        }
        Map<Long, Integer> firstOccurrence = new HashMap<>();
        long[] dictionary = new long[column.length];
        int distinct = 0;
        int[] indexArray = new int[column.length];
        for (int i = 0; i < column.length; i++) {
            Integer id = firstOccurrence.get(column[i]);
            if (id == null) {
                id = distinct;
                firstOccurrence.put(column[i], id);
                dictionary[distinct] = column[i];
                distinct++;
            }
            indexArray[i] = id;
        }
        int width = distinct <= 1 ? 0 : 64 - Long.numberOfLeadingZeros(distinct - 1);
        long[] narrowed = new long[column.length];
        for (int i = 0; i < column.length; i++) {
            narrowed[i] = indexArray[i];
        }
        return new DictionaryEncoding(Arrays.copyOf(dictionary, distinct),
                BitPacking.pack(narrowed, width));
    }

    /** 取第 i 个原始值。 */
    public long valueAt(int index) {
        return dictionary[(int) indices.get(index)];
    }

    /** 解码全列。 */
    public long[] decode() {
        long[] out = new long[size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = valueAt(i);
        }
        return out;
    }

    /** 字典表副本（首次出现序）。 */
    public long[] dictionary() {
        return dictionary.clone();
    }

    /** 去重数读数。 */
    public int distinctCount() {
        return dictionary.length;
    }

    /** 下标位宽读数（⌈log₂(distinct)⌉；单值 0）。 */
    public int indexBitWidth() {
        return indices.bitWidth();
    }

    /** 列长读数。 */
    public int size() {
        return indices.count();
    }

    /** 下标列打包字数读数（压缩对账）。 */
    public int indicesWordCount() {
        return indices.wordCount();
    }
}
