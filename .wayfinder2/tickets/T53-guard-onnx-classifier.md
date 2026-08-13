---
id: T53
title: guard · 分层分类器（ONNX Prompt-Guard，默认关）
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

确定性防御（spotlighting/canary/写门）之上要不要加概率性分类器层？事实源：onnxruntime（21,369★ 达标**承载源**、官方 Java API）；Prompt Guard 2（22M/86M，HF gated 注记：检测注入+越狱、社区广泛 ONNX 化）；Llama-Guard-3-8B（8B 后置审核）；WARD 等评测显示此类分类器**可被绕过**（只作纵深一层）；模型本体不在 GitHub（llama-models 7,678★ 不达标）。

## 待定决策（研究推荐已备）

1. `InjectionClassifier` 接口 + 默认 `OnnxPromptGuard`（onnxruntime Java **optional 依赖** + 模型文件探测式加载，gated license **用户自备下载**）——采纳。
2. 8B 后置分类器委托外部推理服务（OpenAI 兼容端点）——接口预留、实现可选。
3. **默认关**（Buzhou 已有确定性 spotlighting+canary，分类器是概率层；开启须显式配置）——采纳。
4. 模型分发细节（下载路径/校验和/版本钉住）——fog，实现时定。

依据：`docs/research/oss-perfect-tier23.md` §5.6（ONNX 86M 路径 3–4 天，ROI 中）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §guard-25**（用户常设授权 2026-08-14 ratify、可推翻）。InjectionClassifier+OnnxPromptGuard（onnxruntime optional、模型自备、默认关）；8B 后置接口预留。
