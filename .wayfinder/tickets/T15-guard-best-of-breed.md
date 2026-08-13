---
id: T15
title: guard 对标开源最优——读侧 spotlighting/canary/Cedar 等 best-of-breed 落地
type: task
status: open
assignee: ""
blocked-by: [T11]
created: 2026-08-13
---

## Question

把 [T11](T11-oss-best-ideas-core-memory-spill-guard.md) / `docs/research/oss-best-of-breed.md` §4 §5 里 **guard** 的 best-of-breed 思想择优写入。重点守住并放大「读写非对称 + 确定性事实采集」两件真原创，并补齐读回路径的间接注入防御。落地项：

### Tier 1（最高 ROI，先做）
- **读侧 Spotlighting**（MSRC：Datamarking + Encoding + Delimiting）：读侧 offload 把工具/RAG 输出回灌 prompt 时包随机分隔符 + 交织标记；system prompt 指示模型把标记段当纯数据。**最廉价高 ROI，直击 #1 威胁。**
- **canary-token 泄漏检测**（Rebuff）：prompt 注密语，`afterTool` 跑 `is_canary_leaked(output)`；泄漏则阻断 + embedding 入拒识向量（自硬化）。
- **Guardrails `on_fail` 动词汇**作读写通道统一语言：读侧默认 `FILTER`/`REFRAIN`（降级透传）、写侧默认 `EXCEPTION`（阻断）、可恢复 schema 失败 `REASK`（错误回喂）。完美映射既有读写非对称。

### Tier 2
- **Cedar 策略引擎**作 HITL 授权（Strands/AWS）：把 config-driven gate bespoke 代码换成默认拒、数学可分析的 policy DSL；`permit when { context.session.human_approved }`，授权旗标在 session state。
- 分层分类器：Prompt-Guard 类检测器 `beforeModel` 前置 + Llama-Guard 类审核后置（避 Llama Guard 间接/agentic 注入盲区）。
- 工具参数 Pydantic schema 校验 + 失败重试（Instructor/Guardrails）。
- `run_command` 走 Firecracker microVM 沙箱 + 网络出网 allowlist + secret 脱敏（E2B/Deno）。

### Tier 3（冲极致）
- **FIDES 信息流控制**（MSRC 研究）：读侧数据打 tainted 标签，`beforeTool/beforeModel` 强制 tainted 内容未经消毒/审批不得流入写侧工具——读写非对称的形式正确性终点。
- ECDSA 签名审计日志（IETF AAT 草案）：HITL 决策 + 工具调用带 principal/action/args/decision/签名。
- CI 自动红队门（Promptfoo/PyRIT/Garak）：pre-release 回归断言攻击成功率。

## Context

- **已领先（勿重做）= 真原创**：读侧 offload / 写侧 onload **失败非对称**（无任何 survey 框架表达此）；HITL **session-state 授权**（强于 Cline 模型判 / Claude Code 静态规则）；**确定性事实采集 hook→state→attachment**（构造性抗注入，堵死 Letta/Unit-42 记忆投毒——Letta 的 LLM 自管记忆是已建档攻击面）。
- **专项：读回路径间接注入**（spill/guard 交汇，最高风险面）分层防御序：①Spotlighting → ②canary → ③检索/输入 rail 消毒 → ④Prompt-Guard 检测器 → ⑤FIDES taint → ⑥写侧 HITL 门（已有，最后确定性阻断）。
- 反模式：仅靠单一审核模型（Llama Guard 自我否认）；让模型决定命令是否需审批；LLM 自管长期记忆无 taint；V8/容器沙箱跑联网不可信代码；`REASK` 无上限；shell 静态 denylist；闭源引擎作唯一控制。
- 详源见 `docs/research/oss-best-of-breed.md` §4 §4.5 与 §6（NeMo / Guardrails AI / Llama Guard / Lakera / Rebuff / Cline / Claude Code / Cedar / Promptfoo / E2B / Deno / Instructor / **MSRC** / LangGraph / Letta / **Unit 42** / IETF AAT）。

## Resolution
<!-- 实现后填 -->

## Resolution（Tier-1 部分，2026-08-13）

Tier-1 三项全部落地并闭合于细化票：[读侧 Spotlighting + canary 自硬化](T18-guard-read-side-injection-defense.md)、[on_fail 动词汇](T19-guard-on-fail-vocabulary.md)。纵深序 ①spotlighting→②canary 已就位，③检索消毒→④检测器→⑤FIDES（Tier-2/3）与 Cedar HITL/分层分类器/沙箱继续由本票追踪。
