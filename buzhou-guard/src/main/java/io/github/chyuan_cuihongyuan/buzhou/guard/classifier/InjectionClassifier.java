package io.github.chyuan_cuihongyuan.buzhou.guard.classifier;

/**
 * 注入分类器接口（wayfinder2 impl-24 / T53 / docs/spec/12 §guard-25）：
 * 概率性注入/越狱检测层——确定性防御（spotlighting/canary/写门）之上的<b>纵深一层</b>，
 * 非替代（WARD 等评测：此类分类器可被绕过）。默认关。
 */
public interface InjectionClassifier {

    /** 检测结果：分数 ∈ [0,1]（≥ threshold 判阳）+ 类别。 */
    record Verdict(boolean injectionDetected, double score, String label) {
    }

    /**
     * 是否可用（模型/运行时缺失时 false——探测式降级，不误报不静默拦）。
     */
    boolean available();

    /** 检测文本。实现应自身处理不可用状态（返回 not-detected 并标记 degraded）。 */
    Verdict classify(String text);

    /** 默认判阳阈值（86M 级模型社区惯例 ~0.5；可按部署校准）。 */
    double DEFAULT_THRESHOLD = 0.5;
}
