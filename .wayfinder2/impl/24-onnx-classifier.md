# 24 — guard · 分层分类器（ONNX Prompt-Guard，默认关）

**What to build:** 确定性防御之上可加挂概率性注入分类器层：InjectionClassifier 接口 + ONNX Prompt-Guard 默认实现（onnxruntime optional、模型自备、默认关）。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] `InjectionClassifier` 接口（输入文本→注入/越狱判定+分数）
- [ ] `OnnxPromptGuard` 默认实现：onnxruntime Java **optional 依赖** + 模型文件探测式加载（HF gated：**用户自备下载**、路径/校验和/版本钉住文档化）
- [ ] **默认关**：未配置模型时明确降级提示（不误报不静默）
- [ ] 8B 后置分类器：外部推理服务（OpenAI 兼容端点）接口预留
- [ ] 接入 beforeModel 门（作为纵深一层，不替代 spotlighting/canary）
- [ ] 端到端：配置开启后注入样本被标记；未配置时行为不变
- [ ] spec 07（Hook 护栏）同步

> spec 12 §guard-25；[T53](../tickets/T53-guard-onnx-classifier.md)。承载源：onnxruntime 21,369★；模型 HF gated 注记。
