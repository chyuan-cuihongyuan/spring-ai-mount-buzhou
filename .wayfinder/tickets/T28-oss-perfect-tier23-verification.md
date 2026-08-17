---
id: T28
title: 「做完美」第二轮 OSS 核验（10K+ 门槛 + Tier-2/3 深挖）
type: research
status: closed
assignee: wayfinder-chart-session
blocked-by:
created: 2026-08-14
---

## Question

用户要求「参考各家 stars ≥ 10K 的开源项目继续把 core+memory+spill+guard 做深做透做完美」。第一轮研究（[T11](../../.wayfinder/tickets/T11-oss-best-ideas-core-memory-spill-guard.md) → `docs/research/oss-best-of-breed.md`）产出 Tier-1/2/3 backlog 且 Tier-1 已落地；本轮须按 10K+ 门槛**重新核验全部候选事实源**（含 star 数逐仓核验、org 迁移、归档状态），并深挖 Tier-2/3 每项在达标项目中的**实现细节**（API/语义/默认值），为 spec 12 提供事实源。

## Resolution

4 个并行 research 子 agent（core/memory/spill/guard）经 GitHub REST API + 各项目文档/源码（DeepWiki/WebFetch）核验 40+ 仓库，产出 **[docs/research/oss-perfect-tier23.md](../../docs/research/oss-perfect-tier23.md)**。要点：

- **达标核验**：核心源全达标（langchain 144K / cline 66K / autogen 60K / mem0 63K / codex 106K / aider 48K / langgraph 40K / letta 24K / mastra 27K / vercel-ai 26K / temporal 22K / pydantic-ai 19K / instructor 14K / opa 12K / onnxruntime 21K / firecracker 36K / deno 108K / E2B 13K / promptfoo 24K / tree-sitter 27K / graphiti 30K）；注记源=Restate 4.3K、Spring AI 9.3K、MCP spec 仓 8.9K、cedar 系 1.7K/75、NeMo 6.9K、guardrails-ai 7.3K、PyRIT 4.3K、garak 8.8K、JavaParser 6.1K；Rebuff 1.5K 已归档 → 出界。
- **关键修正**：①LangGraph superstep ≠ 整批回滚（pending-writes 半事务）；②Codex 截断=头尾各半掐中间、v0.56+ 转 token-based；③MCP poll_token 非标准；④OPA 无成熟 JVM 内嵌（opa-java 仅 REST 客户端）；⑤Mastra/instructor org 迁移；⑥Letta `core_memory_replace` 唯一性检查=防静默覆写关键；⑦FIDES 论文全文语义已取（join 半格、AgentDojo 注入归零、效用损失 4.5–16.2%）。
- **出界裁决**：MCP widget/poll_token（headless 无 UI）、Rebuff、NeMo/Guardrails-AI/PyRIT/garak 依赖、Temporal/Dapr engine 整体引入。
- **执行序建议**：测试地基（FakeChatModel）先行，详研究文档各模块「实施顺序」节。

（用户常设授权 2026-08-14：按推荐 AFK 收口、可推翻。）
