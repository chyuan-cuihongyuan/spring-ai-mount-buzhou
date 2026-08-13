package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * Embedding 提供者抽象（wayfinder2 impl-15/18 / T41+T46 / docs/spec/12：
 * 向量 recall 与语义回读共享基建——先落地者定形状）。
 *
 * <p>实现由部署侧提供（真模型）；测试以确定性词包向量化（重叠语义可比较）。
 * 维度由实现自定，消费方按向量长度自适应（余弦相似度）。
 */
public interface EmbeddingProvider {

    float[] embed(String text);

    /** 余弦相似度（长度不等/零向量按 0）。 */
    static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0.0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
