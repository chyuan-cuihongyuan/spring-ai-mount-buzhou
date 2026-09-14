package io.github.chyuan_cuihongyuan.buzhou.memory.summary;
/** spec 1529 / T2309：摘要降级 SPI——生成失败/超预算时的段落丢弃阶梯契约。 */

public interface SummaryDegrader {

    NineSectionSummary degradeToFit(NineSectionSummary summary, int maxTokens,
                                    io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator estimator);
}
