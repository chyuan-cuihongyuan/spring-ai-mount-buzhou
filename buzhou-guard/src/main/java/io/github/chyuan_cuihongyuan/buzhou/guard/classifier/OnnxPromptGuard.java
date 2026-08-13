package io.github.chyuan_cuihongyuan.buzhou.guard.classifier;

import java.nio.file.Path;
import java.util.function.ToDoubleFunction;

/**
 * ONNX Prompt-Guard 分类器（wayfinder2 impl-24 / T53）：编排层——阈值判定、降级语义、
 * 可用性探测；<b>推理后端由部署侧注入</b>（onnxruntime Java 为 optional 承载依赖
 * com.microsoft.onnxruntime:onnxruntime，21,369★；HF gated license：<b>模型文件用户自备</b>，
 * 路径/校验和/版本钉住由部署管理——spec 12 fog「模型分发细节」）。
 *
 * <p>探测式降级：classpath 无 onnxruntime / 模型缺失 / 后端异常 → 明确 not-detected +
 * degraded 标签（不误报、不静默拦）。8B 后置审核分类器（外部推理端点）为接口预留。
 */
public final class OnnxPromptGuard implements InjectionClassifier {

    /** 推理后端：文本 → 注入 logits/分数（部署侧以 onnxruntime 加载自备模型实现）。 */
    public interface InferenceBackend {
        double score(String text) throws Exception;
    }

    private final InferenceBackend backend;
    private final double threshold;
    private final boolean runtimePresent;

    public OnnxPromptGuard(Path modelPath, InferenceBackend backend) {
        this(backend, DEFAULT_THRESHOLD, probeRuntime() && modelPath != null);
    }

    public OnnxPromptGuard(InferenceBackend backend, double threshold, boolean modelReady) {
        this.backend = backend;
        this.threshold = threshold;
        // onnxruntime 探测（optional 依赖；类缺失即不可用）+ 模型就绪旗标（部署侧核验后置真）
        this.runtimePresent = modelReady && probeRuntime() && backend != null;
    }

    private static boolean probeRuntime() {
        try {
            Class.forName("ai.onnxruntime.OrtEnvironment");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public boolean available() {
        return runtimePresent;
    }

    @Override
    public Verdict classify(String text) {
        if (!available()) {
            return new Verdict(false, 0.0, "degraded:unavailable");
        }
        try {
            double score = backend.score(text);
            boolean detected = score >= threshold;
            return new Verdict(detected, score, detected ? "injection" : "benign");
        } catch (Exception e) {
            return new Verdict(false, 0.0, "degraded:error:" + e.getClass().getSimpleName());
        }
    }
}
