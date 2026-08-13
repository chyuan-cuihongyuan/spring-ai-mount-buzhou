---
id: T44
title: spill · context-clearing（已消费 tool_result → Handle + 显式逐出）
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

Anthropic 判定「清除深层历史已消费 tool_result 是最安全、最轻的压缩」——Buzhou 如何 harness 自持地做？事实源：Anthropic context engineering（注记：「safest lightest touch」、JIT=只持轻量标识、元数据是导航信号）；Claude API `context_management.edits[] = clear_tool_uses_20250919`（注记：server 侧**仅 Anthropic**、只清 tool_result 保消息结构、**cache miss 代价**——断点放置重要）。

## 待定决策（研究推荐已备）

1. 自实现 ConversationPostProcessor：上下文超阈值时把**旧 tool_result 替换为 Handle 占位**（"cleared; refetch via ReadRangeTool(evidence-id)"）、保最近 N 个完整——采纳（**跨 provider 由 harness 自持，对所有模型生效**——这是超越 Claude API server 侧方案的关键差异）。
2. 显式逐出：`EvictHandleTool`（模型主动）+ 引用计数 TTL（框架自动）双路径——采纳。
3. cache 断点意识：清除动作在消息窗尾部边界批量做、避免每 turn 增量改写（防 cache miss 放大）——spec 定策略。
4. 与 hot-tail（T21）协同：hot-tail 管「新结果何时溢出」、clearing 管「旧 tool_result 何时降级为句柄」——spec 定边界。

依据：`docs/research/oss-perfect-tier23.md` §4.2（工作量中，ROI 高：Handle 价值闭环的关键）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §spill-16**（用户常设授权 2026-08-14 ratify、可推翻）。旧 tool_result→Handle 占位保最近 N 完整；EvictHandleTool+TTL 双路径；harness 自持跨 provider。
