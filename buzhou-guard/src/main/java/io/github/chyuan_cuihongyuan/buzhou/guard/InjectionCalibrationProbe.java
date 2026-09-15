package io.github.chyuan_cuihongyuan.buzhou.guard;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 注入分类校准探针（L 会话 1700 系 R42 = effort #1741 / spec 1741 /
 * 票 T2683 + T2684 / impl 1341）——HuggingFace evaluate / 混淆矩阵校准
 * 思想：{@link InjectionClassifier} 的判定与人工标注金标的对照抽查——
 * 误报（放走注入）与误杀（拦真请求）的代价不对称，precision/recall
 * 分开看才校准得了阈值。{@link PiiProbeSelfCheck}（PII 同族先例）。
 *
 * <p>实例面线程安全：`record(predictedMalicious, actuallyMalicious)`
 * 四象限归账（TP/TN/FP/FN）+`report()` 吐 precision/recall/accuracy
 * （分母 0 哨兵 −1）+resetForTest。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class InjectionCalibrationProbe {

    private final AtomicLong tp = new AtomicLong();
    private final AtomicLong tn = new AtomicLong();
    private final AtomicLong fp = new AtomicLong();
    private final AtomicLong fn = new AtomicLong();

    /** 记一次对照（predicted=分类器判定；actually=金标）。 */
    public void record(boolean predictedMalicious, boolean actuallyMalicious) {
        if (predictedMalicious && actuallyMalicious) {
            tp.incrementAndGet();
        } else if (predictedMalicious) {
            fp.incrementAndGet();
        } else if (actuallyMalicious) {
            fn.incrementAndGet();
        } else {
            tn.incrementAndGet();
        }
    }

    /**
     * @param samples  样本总数
     * @param tp       真阳
     * @param fp       误报（误杀真请求）
     * @param fn       漏报（放走注入）
     * @param tn       真阴
     * @param precision 精确率 tp/(tp+fp)；分母 0 哨兵 −1
     * @param recall    召回率 tp/(tp+fn)；分母 0 哨兵 −1
     * @param accuracy  准确率 (tp+tn)/samples；无样本哨兵 −1
     */
    public record CalibrationReport(long samples, long tp, long fp, long fn, long tn,
                                    double precision, double recall, double accuracy) {
    }

    /** 快照。 */
    public CalibrationReport report() {
        long tps = tp.get();
        long fps = fp.get();
        long fns = fn.get();
        long tns = tn.get();
        long samples = tps + fps + fns + tns;
        double precision = tps + fps == 0 ? -1d : (double) tps / (tps + fps);
        double recall = tps + fns == 0 ? -1d : (double) tps / (tps + fns);
        double accuracy = samples == 0 ? -1d : (double) (tps + tns) / samples;
        return new CalibrationReport(samples, tps, fps, fns, tns,
                precision, recall, accuracy);
    }

    /** 测试归零。 */
    public void resetForTest() {
        tp.set(0);
        tn.set(0);
        fp.set(0);
        fn.set(0);
    }
}
