---
id: T31
title: core · 显式取消 CancelMode 三档 + token 贯穿
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

取消一个运行中的 Session/Turn 时，「立即 / 等当前工具 / 等当前 Turn」三档语义如何建模，取消如何贯穿嵌套工具链并防止部分更新泄漏？事实源：AutoGen（60,404★：`CancellationToken` 贯穿 + partial 丢弃 + `ExternalTermination` 优雅档）、OpenAI Agents SDK（28,616★：cancel 后须继续消费 stream 完成清理、部分结果可挽救）；Pydantic AI 补充 `RunCancelled` 终态。

## 待定决策（研究推荐已备）

1. `enum CancelMode { IMMEDIATE, AFTER_CURRENT_TOOLS, AFTER_CURRENT_TURN }`（AutoGen 两档 + 中间档）——采纳；JDK21 下 IMMEDIATE=虚拟线程 interrupt、AFTER_CURRENT_TOOLS=等 StructuredTaskScope join。
2. 取消 token 贯穿 TurnLoop 与工具执行器，`InterruptibleTool` 可选接口让长任务主动检查——采纳。
3. 落盘策略：AFTER_CURRENT_TURN 保留部分输出入 Completed-Turn；IMMEDIATE 丢弃在飞工具结果（**吸取 AutoGen「partial 丢弃」防半成品泄漏**）——采纳。

依据：`docs/research/oss-perfect-tier23.md` §2.3（约 1 周，ROI 高，实现面积小语义清晰）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §core-3**（用户常设授权 2026-08-14 ratify、可推翻）。CancelMode 三档+token 贯穿；IMMEDIATE 丢在飞结果、TURN 后档保留部分输出入 Completed-Turn。
