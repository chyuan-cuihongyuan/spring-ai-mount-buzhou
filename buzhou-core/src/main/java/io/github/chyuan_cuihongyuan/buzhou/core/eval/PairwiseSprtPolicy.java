package io.github.chyuan_cuihongyuan.buzhou.core.eval;

/**
 * A/B 成对评估的 SPRT 序贯判定器（spec 1605 / T2361，Wald 序贯概率比检验思想——
 * 现代 A/B 平台 GrowthBook/Statsig 的 sequential testing 同源）：符号检验口径
 * （每对非平局项计 +1/-1，平局不进检验），LLR 越界即提前得出方向结论——显著优势
 * 早现时剩余项不必再跑，评估成本（双 runtime 执行 + judge 裁决）按需支付。
 *
 * <p>边界：{@code LLR ≥ ln((1-β)/α)} → PREFER_A；{@code LLR ≤ ln(β/(1-α))} →
 * PREFER_B；否则 CONTINUE。α=假阳性率、β=假阴性率（均 ∈ (0, 0.5]，默认 0.05/0.10）。
 * 诚实边界：SPRT 假定项序随机/可交换——数据集构建序即序贯序，系统性排序偏差会
 * 破坏检验效力（结论保守但有效性依赖数据集纪律）。
 */
public final class PairwiseSprtPolicy {

    /** 序贯判定三态（CONTINUE=证据不足继续）。 */
    public enum Decision {CONTINUE, PREFER_A, PREFER_B}

    private static final double DEFAULT_ALPHA = 0.05;
    private static final double DEFAULT_BETA = 0.10;
    private static final double HALF = 0.5;

    private final double upperBound;
    private final double lowerBound;

    /** 默认 α=0.05 / β=0.10。 */
    public static PairwiseSprtPolicy defaults() {
        return new PairwiseSprtPolicy(DEFAULT_ALPHA, DEFAULT_BETA);
    }

    public PairwiseSprtPolicy(double alpha, double beta) {
        if (!(alpha > 0 && alpha <= HALF) || !(beta > 0 && beta <= HALF)) {
            throw new IllegalArgumentException(
                    "SPRT alpha/beta 必须在 (0, 0.5]（当前 alpha=" + alpha + ", beta=" + beta + "）");
        }
        this.upperBound = Math.log((1 - beta) / alpha);
        this.lowerBound = Math.log(beta / (1 - alpha));
    }

    /**
     * 序贯判定：winsA/winsB 为当前累计胜局（平局/错误不计入——符号检验分母 =
     * 非平局数）。零样本恒 CONTINUE（无证据不下结论）。
     */
    public Decision decide(int winsA, int winsB) {
        int n = winsA + winsB;
        if (n == 0) {
            return Decision.CONTINUE;
        }
        double pHat = (double) winsA / n;
        // winsX=0 时 0*log(0) 按极限取 0（符号检验 LLR 约定）；MLE 双侧需方向分离——
        // p̂ 偏离 0.5 的证据强度取绝对值、方向由 sign 决定（B 全胜不得算成 A 优）
        double magnitude = (winsA == 0 ? 0 : winsA * Math.log(2 * pHat))
                + (winsB == 0 ? 0 : winsB * Math.log(2 * (1 - pHat)));
        double llr = pHat >= HALF ? magnitude : -magnitude;
        if (llr >= upperBound) {
            return Decision.PREFER_A;
        }
        if (llr <= lowerBound) {
            return Decision.PREFER_B;
        }
        return Decision.CONTINUE;
    }

    /** 上界读数（ln((1-β)/α)——测试/观测面）。 */
    public double upperBound() {
        return upperBound;
    }
}
