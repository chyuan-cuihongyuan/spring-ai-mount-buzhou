---
id: T33
title: core · 事件溯源工具调用日志 + 幂等键
type: task
status: closed
assignee: ""
blocked-by: T32
created: 2026-08-14
---

## Question

如何让「crash-safe + exactly-once」从口号变真话？事实源：Temporal（22,284★：Event History 唯一事实源、replay 复用 Activity 结果不重算、写型 Activity 应幂等、WorkflowId 去重）、Dapr（26,021★：同款 history+replay、非确定性检测）；Restate（4,287★ 注记：idempotency-key→稳定 invocation id，语义最贴 agent）。

## 待定决策（研究推荐已备）

1. **不引入 workflow engine**，取两点：(a) 追加式 `ToolCallLog`（turnId、toolCallId、请求指纹、outcome），restart 时已落盘 outcome 的 toolCall **按 id 短路不重跑**；(b) 幂等键 = `sessionId+turnId+toolCallId` 随调用传给工具端——采纳。
2. **replay 不重跑 LLM**：以 Completed-Turn 为恢复点、恢复=最后 Completed-Turn 之后续跑，天然规避 Temporal 式全量重放（agent 场景比 workflow 更友好之处）——采纳。
3. 与 Run 注册表（T32）的关系：ToolCallLog 是 RunRegistry 快照的**前置证据层**，同存储介质（JDBC/InMemory）——spec 定 schema。
4. 请求指纹（argsHash）算法与碰撞处理——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §2.5（1–2 周，ROI 中高；Diagrid 批判「checkpoint≠durable」的正解）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §core-5**（用户常设授权 2026-08-14 ratify、可推翻）。追加式 ToolCallLog（argsHash 指纹+outcome）+ 幂等键；恢复=Completed-Turn 后续跑、不重放 LLM。
