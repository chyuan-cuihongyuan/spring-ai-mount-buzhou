package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.List;

/**
 * Embedding 提供者质量自查探针（spec 1423 / T2147 / impl 1076）——OpenAI
 * embeddings cookbook / sentence-transformers 语义自检思想：部署侧换模型/
 * 换供应商后，检索质量的劣化最先表现在<b>已知相似对的余弦序</b>上——
 * 相似文本对的相似度必须显著高于无关文本对。本探针用确定性合成句对穿测
 * 任意 {@link EmbeddingProvider}，报告序正确性与最小间隔，回归从「检索
 * 全线莫名劣化」变成「一行自查显形」。
 *
 * <p>纯函数零状态：不落台账不接线；样本为合成语义对（同义改写 vs 无关
 * 句），不依赖具体词表——实现换成真模型/测试词包均可穿测。序判定（而非
 * 绝对阈值）是口径核心：余弦绝对值随模型分布漂移，但相似对的<b>相对序</b>
 * 在任何可用模型上都应成立。
 */
public final class EmbeddingSelfCheck {

    private EmbeddingSelfCheck() {
    }

    /** 已知相似句对（语义近邻——合成词包口径下共享语义关键词）。 */
    static final List<String[]> SIMILAR_PAIRS = List.of(
            new String[]{"用户想退货", "买家申请退货"},
            new String[]{"今天天气很好", "今天天气不错"});

    /** 已知无关句对（跨话题——语义远邻，与相似对一一对照）。 */
    static final List<String[]> DISSIMILAR_PAIRS = List.of(
            new String[]{"用户想退货", "机房空调维护"},
            new String[]{"今天天气很好", "季度财报审计"});

    /**
     * @param similarPairsPassed  相似对通过数（cos(similar) &gt; cos(dissimilar 对照)）
     * @param similarPairsTotal   相似对总数
     * @param minMargin           最小间隔（min[cos(similar) − cos(dissimilar)]；&gt;0 即序成立）
     * @param dimension           提供者输出维度（首向量；0 = 无样本）
     */
    public record ProbeReport(int similarPairsPassed, int similarPairsTotal,
                              double minMargin, int dimension) {

        /** 序完全成立（所有相似对通过且最小间隔 &gt; 0）。 */
        public boolean orderHolds() {
            return similarPairsTotal > 0 && similarPairsPassed == similarPairsTotal
                    && minMargin > 0;
        }
    }

    /** 穿测入口：对 provider 跑全部合成对。 */
    public static ProbeReport probe(EmbeddingProvider provider) {
        int passed = 0;
        double minMargin = Double.MAX_VALUE;
        int dimension = 0;
        for (int i = 0; i < SIMILAR_PAIRS.size(); i++) {
            String[] sim = SIMILAR_PAIRS.get(i);
            String[] dis = DISSIMILAR_PAIRS.get(i);
            float[] a = provider.embed(sim[0]);
            float[] b = provider.embed(sim[1]);
            float[] c = provider.embed(dis[1]);
            if (dimension == 0 && a.length > 0) {
                dimension = a.length;
            }
            double simCos = EmbeddingProvider.cosine(a, b);
            double disCos = EmbeddingProvider.cosine(a, c);
            double margin = simCos - disCos;
            if (margin > 0) {
                passed++;
            }
            minMargin = Math.min(minMargin, margin);
        }
        if (minMargin == Double.MAX_VALUE) {
            minMargin = 0;
        }
        return new ProbeReport(passed, SIMILAR_PAIRS.size(), minMargin, dimension);
    }
}
