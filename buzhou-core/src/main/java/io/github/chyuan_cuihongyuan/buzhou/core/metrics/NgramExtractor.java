package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * n-gram 特征提取（spec 2054 / T3209 / impl 1605）——信息检索经典
 *（n-gram 索引/指纹）思想：文本 → 定长滑窗片段集——字符 n-gram
 *（近重复指纹特征供给 SimHash）与词 n-gram（短语级相似）。散落内联
 * 实现（CanaryGuardHook 私有 ngrams 等）统一口径的可收敛原语。
 *
 * <p>纯函数零状态、确定性（保持出现序去重）；文本短于 n 返回原词
 * 单元（诚实边界——不足窗即整段）。
 */
public final class NgramExtractor {

    private NgramExtractor() {
    }

    /**
     * 字符 n-gram：定长滑窗（保持出现序，默认去重）。文本非空但短于
     * n 时返回原文本单元素。契约：text 非 null、n ≥ 1。
     *
     * @param distinct true 去重（指纹特征口径）；false 保留重复（频次口径）
     */
    public static List<String> charNgrams(String text, int n, boolean distinct) {
        if (text == null) {
            throw new IllegalArgumentException("text 不能为 null");
        }
        if (n < 1) {
            throw new IllegalArgumentException("n 须 ≥ 1：" + n);
        }
        if (text.isEmpty()) {
            return List.of();
        }
        List<String> grams = new ArrayList<>();
        for (int i = 0; i + n <= text.length(); i++) {
            grams.add(text.substring(i, i + n));
        }
        if (grams.isEmpty()) {
            return List.of(text); // 不足一窗——整段
        }
        return distinct ? new ArrayList<>(new LinkedHashSet<>(grams)) : grams;
    }

    /**
     * 词 n-gram（空白分词）：定长词窗短语。词数少于 n 返回原词列表；
     * 空白文本返回空。契约：text 非 null、n ≥ 1。
     */
    public static List<String> wordNgrams(String text, int n, boolean distinct) {
        if (text == null) {
            throw new IllegalArgumentException("text 不能为 null");
        }
        if (n < 1) {
            throw new IllegalArgumentException("n 须 ≥ 1：" + n);
        }
        String[] tokens = text.trim().split("\\s+");
        if (tokens.length == 0 || (tokens.length == 1 && tokens[0].isEmpty())) {
            return List.of();
        }
        List<String> grams = new ArrayList<>();
        for (int i = 0; i + n <= tokens.length; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0) {
                    sb.append(' ');
                }
                sb.append(tokens[i + j]);
            }
            grams.add(sb.toString());
        }
        if (grams.isEmpty()) {
            return List.of(tokens); // 词数不足窗——原词
        }
        return distinct ? new ArrayList<>(new LinkedHashSet<>(grams)) : grams;
    }
}
