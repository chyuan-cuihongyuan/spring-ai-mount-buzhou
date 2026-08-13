---
id: T11
title: 各机制「对标开源最优」best-of-breed 技术萃取（core/memory/spill/guard）
type: research
status: closed
assignee: wayfinder-session-2026-08-13
blocked-by: [T2]
created: 2026-08-13
closed: 2026-08-13
---

## Question

用户把目的地（core/memory/spill/guard 做深做透）的**量化锚**定为「对标所有现存开源框架、选最优思想写入」。本票在 [T2](T2-spring-ai-native-vs-buzhou.md)（已判 Spring AI 2.0 原生面）之外，**横扫所有主流开源 Agent/LLM 框架**，逐机制萃取值得 Buzhou 采纳/对标的 best-of-breed 技术思想，产出「业界现状最优 → Buzhou 该采纳哪些 → Buzhou 哪里已领先」清单，喂 [T3 验收基线](T3-depth-definition-of-done.md)。

## Context

- T2 已覆盖 Spring AI 原生（NATIVE/ADDS/REPLACES），**本票不重复 Spring AI**；扩展到 LangGraph / MemGPT(Letta) / LangChain / LlamaIndex / OpenAI Agents SDK / AutoGen / CrewAI / Pydantic AI / Vercel AI SDK / Semantic Kernel / NeMo Guardrails / Guardrails AI / Llama Guard / Lakera / Rebuff / Cline / Claude Code / Aider / Zep 等。
- 由 4 个并行 research 子 agent 分头负责 core / memory / spill / guard。
- 产出落 `docs/research/oss-best-of-breed.md`，作为 T3 的「业界已做到什么程度」参照。

## Resolution

已由 4 个并行 research 子 agent 在本会话解决（core / memory / spill / guard 各一，web 核验）。产出落 **`docs/research/oss-best-of-breed.md`**，作为 [T3 验收基线](T3-depth-definition-of-done.md) 的「业界已做到什么程度」参照与 [T9 边界文档](T9-spring-ai-boundary-doc.md) 的补充事实骨架。

**三句话结论**：
1. **已领先（保住并强化，勿重做）**：并行工具+虚拟线程、悬空调用 reactive 修复、微压缩 evidence-id（业界唯一确定性回读指针）、9 段 P0..P3 段内分级、动态预算、spill 三模回读（survey 最广，JSONPath 基本独一无二）、读写失败非对称（真原创）、HITL session-state 授权、确定性事实采集 hook→state→attachment（构造性抗注入，堵 Letta/Unit-42 记忆投毒）。
2. **最高 ROI 采纳（喂 T3 Tier1）**：core=错误回喂模型+有界 turn 预算；memory=Mem0 ADD/UPDATE/DELETE/NOOP 事实对账+预算渲染给 LLM；spill=hot-tail/cold-storage+per-tool durable override+自描述 stub；guard=读侧 MSRC spotlighting+Rebuff canary+Guardrails `on_fail` 动词汇。
3. **冲极致（喂 T3 Tier3）**：core=事件溯源工具调用日志+幂等键（对标 Temporal/Restate，**唯一让 crash-safe+exactly-once 成真**）；spill=语义回读(第 4 模式)+AST-aware 切片；guard=FIDES 信息流控制+ECDSA 审计+CI 红队门。

**T3 grilling 现在有了量化锚**：每模块「done」=「保住已领先项 + 做完该模块 Tier1 + 至少挑一项 Tier2/Tier3 拉开身位」，并以本表的「Buzhou 已领先」清单作「不与业界重复」的判据。
