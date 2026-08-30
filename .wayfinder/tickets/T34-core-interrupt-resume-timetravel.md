---
id: T34
title: core · HITL interrupt/resume 按 toolCallId + time-travel fork
type: task
status: closed
assignee: ""
blocked-by: T33
created: 2026-08-14
---

## Question

人审暂停（interrupt）与历史分叉（fork）如何做才不踩 LangGraph 反模式？事实源：LangGraph（39,627★：`interrupt()`+`Command(resume=...)`——**已知反模式：resume 时所在 node 从头重执行**、多 pending 须 `{interrupt_id: value}` 映射；`get_state_history()` + `update_state(past_config,…)` fork）。

## 待定决策（研究推荐已备）

1. **以 toolCallId 匹配**（非节点内顺序）：Turn 挂起时记录 `pendingToolCalls[]`（toolCallId、args 指纹）；`resume(toolCallId, payload)` 精确注入对应 ToolResponse、可逐个 resume——采纳。
2. **绝不重放 turn 前段**：未落盘 turn 丢弃重开、同批已执行工具结果按 T35 批记录暂存保留——采纳（直接消除 node 重执行反模式）。
3. time-travel/fork：Completed-Turn 即 checkpoint——`Session.listCompletedTurns()`（history 指纹+元数据）+ `forkFrom(turnId)` 把截至该 turn 的 history 复制到新 sessionId 续跑（Buzhou state=消息列表，无需 channel 版本机制）——采纳。
4. interrupt 与既有 HITL 门（guard DangerousToolGuardHook）的合流点：挂起=门未放行、resume=授权 payload——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §2.7（约 1 周，7+8 捆绑共享检查点设施，ROI 高）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §core-6**（用户常设授权 2026-08-14 ratify、可推翻）。pendingToolCalls[] 按 toolCallId resume、绝不重放 Turn 前段；listCompletedTurns+forkFrom 检查点分叉。
