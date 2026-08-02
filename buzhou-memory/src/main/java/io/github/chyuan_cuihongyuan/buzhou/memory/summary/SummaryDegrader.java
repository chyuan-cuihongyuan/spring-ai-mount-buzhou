package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

public interface SummaryDegrader {

    NineSectionSummary degradeToFit(NineSectionSummary summary, int maxTokens,
                                    io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator estimator);
}
